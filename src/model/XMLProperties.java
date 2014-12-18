package model;

public class XMLProperties {

  String xml;
  
  public XMLProperties(String xml) {
    this.xml = xml;
  }
  
  public String getXML() {
    return xml.replace("\n", " ");
  }
}