package nl.naturalis.geneious.samplesheet;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import static nl.naturalis.geneious.samplesheet.SampleSheetRow.COL_EXTRACTION_METHOD;
import static nl.naturalis.geneious.samplesheet.SampleSheetRow.MIN_CELL_COUNT;

public class SampleSheetRowTest {
  
  @Test
  public void test1() {
    //TestGeneious.initialize();
    assertEquals(COL_EXTRACTION_METHOD,MIN_CELL_COUNT);
  }
  
  @Test
  public void isEmpty_01() {
    
  }

}