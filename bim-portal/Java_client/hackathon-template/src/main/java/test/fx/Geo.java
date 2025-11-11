package test.fx;

import java.io.File;

import com.apstex.ifctoolbox.ifc.IfcProduct;
import com.apstex.ifctoolbox.ifc.IfcRepresentation;
import com.apstex.ifctoolbox.ifc.IfcShapeRepresentation;
import com.apstex.ifctoolbox.ifcmodel.IfcModel;

public class Geo {

	public static void main(String[] args) throws Exception {
		IfcModel ifcModel = new IfcModel();
		ifcModel.readStepFile(new File(
				"C:/Users/Max/Nextcloud/BIM_Deutschland_Hackathon/data/ifc/Brücke_BIM.HH_TBW1_01_P.ifc"));
		for (IfcProduct ifcProduct : ifcModel.getCollection(IfcProduct.class)) {
			if (ifcProduct.getRepresentation() != null) {
				for (IfcRepresentation ifcRepresentation : ifcProduct
						.getRepresentation().getRepresentations()) {
					if (ifcRepresentation instanceof IfcShapeRepresentation) {
						System.out.println(ifcProduct.getName() + ": "
								+ ifcRepresentation.getRepresentationType());
					}
				}
			}
		}

	}

}
