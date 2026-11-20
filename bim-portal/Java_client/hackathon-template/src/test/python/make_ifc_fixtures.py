"""Write the small IFC files IfcModelElementsTest reads, one per schema.

They hold the cases the downloaded sample models lack: enumerated and list values, a quantity set
(skipped), a type's property sets with one overridden on the element, and every way a
PredefinedType comes about.

Needs IfcOpenShell (pip install ifcopenshell). Run from this folder:

    python make_ifc_fixtures.py ../resources/ifc

Every element is an IfcSlab: it has a PredefinedType in all three schemas, and so has IfcSlabType.
The expected values are spelled out in IfcModelElementsTest; change both together.
"""
import pathlib
import sys

import ifcopenshell
import ifcopenshell.guid

SCHEMAS = {"IFC2X3": "ifc2x3", "IFC4": "ifc4", "IFC4X3_ADD2": "ifc4x3"}


def write(schema, path):
    f = ifcopenshell.file(schema=schema)
    history = None
    if schema == "IFC2X3":
        # OwnerHistory is mandatory in IFC2X3.
        person = f.createIfcPerson(None, "Test", None)
        org = f.createIfcOrganization(None, "CheckMate", None)
        user = f.createIfcPersonAndOrganization(person, org)
        app = f.createIfcApplication(org, "1", "make_ifc_fixtures", "make_ifc_fixtures")
        history = f.createIfcOwnerHistory(user, app, None, "ADDED", None, None, None, 0)

    def root(entity, **attributes):
        return f.create_entity(entity, GlobalId=ifcopenshell.guid.new(), OwnerHistory=history, **attributes)

    def pset(name, properties):
        return root("IfcPropertySet", Name=name, HasProperties=properties)

    def single(name, value):
        return f.createIfcPropertySingleValue(name, None, value, None)

    def relate(element, definition):
        root("IfcRelDefinesByProperties", RelatedObjects=[element], RelatingPropertyDefinition=definition)

    def slab(name, predefined_type=None, object_type=None):
        return root("IfcSlab", Name=name, ObjectType=object_type, PredefinedType=predefined_type)

    def slab_type(name, predefined_type, element_type=None, property_sets=None):
        return root(
            "IfcSlabType",
            Name=name,
            PredefinedType=predefined_type,
            ElementType=element_type,
            HasPropertySets=property_sets,
        )

    def type_of(element, element_type):
        # Written the same in every schema; IFC2X3 lists it under IsDefinedBy, IFC4 under IsTypedBy.
        root("IfcRelDefinesByType", RelatedObjects=[element], RelatingType=element_type)

    # 1. Own PredefinedType; single, enumerated and list values; a quantity set that is skipped.
    werte = slab("platte-werte", "FLOOR")
    relate(
        werte,
        pset(
            "Pset_Test",
            [
                single("Einzel", f.createIfcLabel("eins")),
                f.createIfcPropertyEnumeratedValue(
                    "Auswahl", None, [f.createIfcLabel("A"), f.createIfcLabel("B")], None
                ),
                f.createIfcPropertyListValue(
                    "Liste", None, [f.createIfcInteger(1), f.createIfcInteger(2)], None
                ),
            ],
        ),
    )
    relate(
        werte,
        root(
            "IfcElementQuantity",
            Name="Qto_Test",
            Quantities=[f.createIfcQuantityArea("Flaeche", None, None, 12.5)],
        ),
    )

    # 2. PredefinedType from the type; a type property set, one property overridden on the element.
    typ_pset = pset(
        "Pset_Typ",
        [single("Geerbt", f.createIfcLabel("vom Typ")), single("Ueberschrieben", f.createIfcLabel("Typ"))],
    )
    typ = slab("platte-typ", "NOTDEFINED")
    type_of(typ, slab_type("Typ ROOF", "ROOF", property_sets=[typ_pset]))
    relate(typ, pset("Pset_Typ", [single("Ueberschrieben", f.createIfcLabel("Element"))]))

    # 3. User-defined on the element: the ObjectType names it.
    slab("platte-eigen", "USERDEFINED", "Sonderplatte")

    # 4. User-defined on the type: the ElementType names it.
    typ_eigen = slab("platte-typ-eigen")
    type_of(typ_eigen, slab_type("Typ eigen", "USERDEFINED", element_type="Typplatte"))

    # 5. No PredefinedType at all, but an ObjectType: IDS reads that as user-defined too.
    slab("platte-objekttyp", None, "Freitext")

    # Not covered: a relation pointing at a set of property sets (IfcPropertySetDefinitionSet,
    # IFC4 and later). IfcOpenShell 0.9 cannot write one, and real files rarely use it.

    f.write(str(path))


def main():
    out = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else ".")
    for schema, folder in SCHEMAS.items():
        path = out / f"checkmate-cases-{folder}.ifc"
        write(schema, path)
        print("written", path)


if __name__ == "__main__":
    main()
