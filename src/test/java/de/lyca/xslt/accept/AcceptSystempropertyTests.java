package de.lyca.xslt.accept;

import static de.lyca.xslt.ResourceUtils.getSource;
import static de.lyca.xslt.ResourceUtils.readResource;
import static java.nio.charset.StandardCharsets.UTF_8;

import javax.xml.transform.Source;

import org.junit.Assert;
import org.junit.Test;

import de.lyca.xslt.Transform;

public class AcceptSystempropertyTests {

  private static final String PACKAGE = '/' + AcceptSystempropertyTests.class.getPackage().getName().replace('.', '/')
          + "/systemproperty/";

  @Test
  public void systemproperty01() throws Exception {
    final String name = PACKAGE + "systemproperty01";
    final Source xsl = getSource(name + ".xsl");
    final Source xml = getSource(name + ".xml");
    final String expected = readResource(name + ".out", UTF_8);
    final Transform t = new Transform(xml, xsl);
    Assert.assertEquals(expected, t.getResultString());
  }

  @Test
  public void systemproperty02() throws Exception {
    final String name = PACKAGE + "systemproperty02";
    final Source xsl = getSource(name + ".xsl");
    final Source xml = getSource(name + ".xml");
    final String expected = readResource(name + ".out", UTF_8);
    final Transform t = new Transform(xml, xsl);
    Assert.assertEquals(expected, t.getResultString());
  }

}
