package test.fx;

import com.apstex.ifctoolbox.ifc.IfcProduct;
import com.apstex.ifctoolbox.ifc.IfcRepresentation;
import com.apstex.ifctoolbox.ifc.IfcShapeRepresentation;
import com.apstex.ifctoolbox.ifcmodel.IfcModel;

public class Geo {

  public static void main(String[] args) throws Exception {
    IfcModel ifcModel = IfcResources.loadDefaultModel();
    for (IfcProduct ifcProduct : ifcModel.getCollection(IfcProduct.class)) {
      if (ifcProduct.getRepresentation() != null) {
        for (IfcRepresentation ifcRepresentation :
            ifcProduct.getRepresentation().getRepresentations()) {
          if (ifcRepresentation instanceof IfcShapeRepresentation) {
            System.out.println(
                ifcProduct.getName() + ": " + ifcRepresentation.getRepresentationType());
          }
        }
      }
    }
  }
}
