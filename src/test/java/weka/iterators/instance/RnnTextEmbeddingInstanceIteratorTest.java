/*
 * WekaDeeplearning4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WekaDeeplearning4j is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WekaDeeplearning4j.  If not, see <https://www.gnu.org/licenses/>.
 *
 * RnnTextEmbeddingInstanceIteratorTest.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.iterators.instance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.log4j.Log4j2;
import org.junit.Before;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import weka.core.Instances;
import weka.dl4j.iterators.instance.sequence.text.rnn.RnnTextEmbeddingInstanceIterator;
import weka.util.DatasetLoader;
import weka.util.TestUtil;

/**
 * JUnit tests for the {@link RnnTextEmbeddingInstanceIterator}
 *
 * @author Steven Lang
 */
@Log4j2
public class RnnTextEmbeddingInstanceIteratorTest {
  /** ImageInstanceIterator object */
  private RnnTextEmbeddingInstanceIterator tii;
  /** WordVec size */
  private static final int WORD_VEC_SIZE = 300;
  /** Initialize iterator */
  @Before
  public void init() {
    this.tii = new RnnTextEmbeddingInstanceIterator();
    final String modelPath = "/home/slang/Downloads/GoogleNews-vectors-negative300-SLIM.bin.gz";
    this.tii.setWordVectorLocation(new File(modelPath));
    this.tii.setTrainBatchSize(10);
  }

  /**
   * Test validate method with valid data
   *
   * @throws Exception Could not load mnist meta data
   */
  @Test
  public void testValidateValidData() throws Exception {
    // Test valid setup
    final Instances metaData = DatasetLoader.loadReutersMinimal();
    this.tii.validate(metaData);
  }

  @Test
  public void testOutputFormat() throws Exception {
    Instances data = DatasetLoader.loadReutersMinimal();
    for (int tl : Arrays.asList(10, 50, 200)) {
      tii.setTruncateLength(tl);
      for (int bs : Arrays.asList(1, 4, 8, 16)) {
        final DataSetIterator it = tii.getDataSetIterator(data, TestUtil.SEED, bs);
        assertEquals(bs, it.batch());
        assertEquals(Arrays.asList("0", "1"), it.getLabels());
        final DataSet next = it.next();

        // Check feature shape, expect: (batchsize x wordvecsize x sequencelength)
        final long[] shapeFeats = next.getFeatures().shape();
        final long[] expShapeFeats = {bs, WORD_VEC_SIZE, tl};
        assertEquals(expShapeFeats[0],shapeFeats[0]);
        assertEquals(expShapeFeats[1],shapeFeats[1]);
        assertTrue(expShapeFeats[2] >= shapeFeats[2]);

        // Check label shape, expect: (batchsize x numclasses x sequencelength)
        final long[] shapeLabels = next.getLabels().shape();
        final long[] expShapeLabels = {bs, data.numClasses(), tl};
        assertEquals(expShapeLabels[0], shapeLabels[0]);
        assertEquals(expShapeLabels[1], shapeLabels[1]);
        assertTrue(expShapeLabels[2] >= shapeLabels[2]);
      }
    }
  }

  @Test
  public void testNominalEncoding() throws Exception {
    Instances data = DatasetLoader.loadReutersMinimal();
    // Check if labels were set correctly
    final DataSetIterator itSingle = tii.getDataSetIterator(data, TestUtil.SEED, 1);
    data.forEach(
        inst -> {
          final double expected = inst.classValue();
          final INDArray lbls = itSingle.next().getLabels();
          final double actual = lbls.getDouble(0, 1, lbls.shape()[2] - 1);
          assertEquals(expected, actual, 10e-5);
        });
  }

  /**
   * Test different word vector formats crafted by hand.
   */
  @Test
  public void testDifferentEmbeddings() {
    File embDir = new File("src/test/resources/embeddings/small");
    final File[] embeddings = embDir.listFiles();
    Set<String> words = new HashSet<>();
    words.add("snowball");
    words.add("christmas");
    words.add("tree");

    for (File f : embeddings) {
      log.info("Testing embedding {}", f.getAbsolutePath());
      RnnTextEmbeddingInstanceIterator teii = new RnnTextEmbeddingInstanceIterator();
      if (f.getAbsolutePath().contains("arff")) {
        log.info("");
      }
      teii.setWordVectorLocation(f);

      teii.initialize();
      final Collection ws = teii.getWordVectors().vocab().words();
      assertTrue(ws.containsAll(words) && words.containsAll(ws));
    }
  }
}
