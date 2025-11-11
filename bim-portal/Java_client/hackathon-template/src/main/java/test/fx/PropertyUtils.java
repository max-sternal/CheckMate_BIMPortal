package test.fx;

import com.apstex.ifctoolbox.ifc.IfcBuildingElement;
import com.apstex.ifctoolbox.ifc.IfcComplexProperty;
import com.apstex.ifctoolbox.ifc.IfcElementQuantity;
import com.apstex.ifctoolbox.ifc.IfcProperty;
import com.apstex.ifctoolbox.ifc.IfcPropertyBoundedValue;
import com.apstex.ifctoolbox.ifc.IfcPropertyEnumeratedValue;
import com.apstex.ifctoolbox.ifc.IfcPropertyListValue;
import com.apstex.ifctoolbox.ifc.IfcPropertyReferenceValue;
import com.apstex.ifctoolbox.ifc.IfcPropertySet;
import com.apstex.ifctoolbox.ifc.IfcPropertySetDefinition;
import com.apstex.ifctoolbox.ifc.IfcPropertySingleValue;
import com.apstex.ifctoolbox.ifc.IfcPropertyTableValue;
import com.apstex.ifctoolbox.ifc.IfcQuantityArea;
import com.apstex.ifctoolbox.ifc.IfcQuantityLength;
import com.apstex.ifctoolbox.ifc.IfcQuantityVolume;
import com.apstex.ifctoolbox.ifc.IfcRelDefinesByProperties;

public class PropertyUtils {

	/**
	 * Append basic info of an IfcBuildingElement to the provided StringBuilder.
	 */
	public static String appendElelemtInfo(IfcBuildingElement be) {

		StringBuilder tabContent = new StringBuilder();
		tabContent.append(be.getStepLine()).append("\n\n");
		tabContent.append("GlobalId: ").append(be.getGlobalId()).append("\n");
		tabContent.append("Name: ").append(be.getName()).append("\n");
		tabContent.append("Description: ").append(be.getDescription())
				.append("\n");
		tabContent.append("ObjectType: ").append(be.getObjectType())
				.append("\n");
		// tabContent.append("PredefinedType: ").append(switch (be) {
		// case IfcWall w -> w.getPredefinedType();
		// case IfcDoor d -> d.getPredefinedType();
		// case IfcWindow w -> w.getPredefinedType();
		// default -> null;
		// }).append("\n\n");

		return tabContent.toString();
	}

	/**
	 * Recursively append property info to the provided StringBuilder with
	 * indentation.
	 */
	public static String appendPropertyInfo(IfcProperty p, String indent) {

		StringBuilder info = new StringBuilder();
		info.append(indent).append("- ").append(p.getName()).append(": ");
		switch (p) {
			case IfcPropertySingleValue sv ->
				info.append(sv.getNominalValue()).append("\n");
			case IfcPropertyEnumeratedValue ev -> info.append("[Enum] ")
					.append(ev.getEnumerationValues()).append("\n");
			case IfcPropertyListValue lv ->
				info.append("[List] ").append(lv.getListValues()).append("\n");
			case IfcPropertyBoundedValue bv -> info.append("[Bounded] Min: ")
					.append(bv.getLowerBoundValue()).append(", Max: ")
					.append(bv.getUpperBoundValue()).append("\n");
			case IfcPropertyReferenceValue rv -> info.append("[Reference] ")
					.append(rv.getPropertyReference()).append("\n");
			case IfcPropertyTableValue tv -> info.append("[Table] Defining: ")
					.append(tv.getDefiningValues()).append(", Defined: ")
					.append(tv.getDefinedValues()).append("\n");
			case IfcComplexProperty cp -> {
				info.append("[Complex]\n");
				cp.getHasProperties().forEach(sub -> info
						.append(appendPropertyInfo(sub, indent + "    ")));
			}
			default -> info.append("[Other Property Type]\n");
		}

		return info.toString();

	}

	/**
	 * List properties and quantities from an IfcRelDefinesByProperties
	 * relationship (IFC2x3 version).
	 */
	public static String listPropertiesAndQuantities(
			IfcRelDefinesByProperties.Ifc2x3 rel) {

		StringBuilder tabContent = new StringBuilder();

		IfcPropertySetDefinition.Ifc2x3 psd = rel
				.getRelatingPropertyDefinition();
		switch (psd) {
			case IfcPropertySet ps -> {
				tabContent.append("P_Set: ").append(ps.getName()).append("\n");
				ps.getHasProperties().forEach(
						p -> tabContent.append(appendPropertyInfo(p, "\t\t")));
			}
			case IfcElementQuantity eq -> {
				tabContent.append("Q_Set: ").append(eq.getName()).append("\n");
				eq.getQuantities().forEach(q -> tabContent.append("\t\t- ")
						.append(q.getName()).append(": ").append(switch (q) {
							case IfcQuantityArea qa -> qa.getAreaValue();
							case IfcQuantityLength ql -> ql.getLengthValue();
							case IfcQuantityVolume qv -> qv.getVolumeValue();
							default -> "[Other Quantity Type]";
						}).append("\n"));
			}
			default -> tabContent.append(psd.getClassName()).append(": ")
					.append(psd.getName()).append("\n");
		}
		return tabContent.toString();
	}

}