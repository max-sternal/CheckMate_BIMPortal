package com.bimportal.hackathon.ifc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bimportal.hackathon.ids.ModelElement;
import com.bimportal.hackathon.ids.ModelElement.ModelProperty;
import com.bimportal.hackathon.ids.PropertyKey;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for {@link StepParser}, {@link IfcModelElements} edge cases and ifcZIP handling. */
class StepParserTest {

  @TempDir Path folder;

  @Test
  void readsLogicalBooleanNumbersBinaryEmptyAndNullPropertyValues() {
    String step =
        ifc4(
            """
            #1=IFCSLAB('0000000000000000000001',$,'Platte',$,$,$,$,$,.FLOOR.);
            #10=IFCPROPERTYSINGLEVALUE('LogU',$,IFCLOGICAL(.U.),$);
            #11=IFCPROPERTYSINGLEVALUE('LogT',$,IFCLOGICAL(.T.),$);
            #12=IFCPROPERTYSINGLEVALUE('LogF',$,IFCLOGICAL(.F.),$);
            #13=IFCPROPERTYSINGLEVALUE('BoolT',$,IFCBOOLEAN(.T.),$);
            #14=IFCPROPERTYSINGLEVALUE('BoolF',$,IFCBOOLEAN(.F.),$);
            #15=IFCPROPERTYSINGLEVALUE('NullVal',$,$,$);
            #16=IFCPROPERTYSINGLEVALUE('EmptyLabel',$,IFCLABEL(''),$);
            #17=IFCPROPERTYSINGLEVALUE('Spaces',$,IFCTEXT('  kept  '),$);
            #18=IFCPROPERTYSINGLEVALUE('HugeReal',$,IFCREAL(1e20),$);
            #19=IFCPROPERTYSINGLEVALUE('WholeReal',$,IFCLENGTHMEASURE(12.0),$);
            #20=IFCPROPERTYSINGLEVALUE('FracReal',$,IFCLENGTHMEASURE(12.5),$);
            #21=IFCPROPERTYSINGLEVALUE('BigInt',$,IFCINTEGER(9007199254740993),$);
            #22=IFCPROPERTYSINGLEVALUE('Bin',$,IFCBINARY("0F"),$);
            #23=IFCPROPERTYSINGLEVALUE('Angle',$,IFCCOMPOUNDPLANEANGLEMEASURE((45,30,15)),$);
            #24=IFCPROPERTYLISTVALUE('ListWithNull',$,(IFCLABEL('a'),$,*,IFCLABEL('b')),$);
            #30=IFCPROPERTYSET('0000000000000000000030',$,'Pset_Edge',$,(#10,#11,#12,#13,#14,#15,#16,#17,#18,#19,#20,#21,#22,#23,#24));
            #31=IFCRELDEFINESBYPROPERTIES('0000000000000000000031',$,$,$,(#1),#30);
            """);

    ModelElement slab = parseElements(step).get(0);

    assertProperty(slab, "Pset_Edge", "LogU", List.of("UNKNOWN"), "IfcLogical");
    assertProperty(slab, "Pset_Edge", "LogT", List.of("true"), "IfcLogical");
    assertProperty(slab, "Pset_Edge", "LogF", List.of("false"), "IfcLogical");
    assertProperty(slab, "Pset_Edge", "BoolT", List.of("true"), "IfcBoolean");
    assertProperty(slab, "Pset_Edge", "BoolF", List.of("false"), "IfcBoolean");
    assertProperty(slab, "Pset_Edge", "NullVal", List.of(), null);
    assertProperty(slab, "Pset_Edge", "EmptyLabel", List.of(""), "IfcLabel");
    assertProperty(slab, "Pset_Edge", "Spaces", List.of("  kept  "), "IfcText");
    assertProperty(slab, "Pset_Edge", "HugeReal", List.of("1.0E20"), "IfcReal");
    assertProperty(slab, "Pset_Edge", "WholeReal", List.of("12"), "IfcLengthMeasure");
    assertProperty(slab, "Pset_Edge", "FracReal", List.of("12.5"), "IfcLengthMeasure");
    assertProperty(slab, "Pset_Edge", "BigInt", List.of("9007199254740993"), "IfcInteger");
    assertProperty(slab, "Pset_Edge", "Bin", List.of("0F"), "IfcBinary");
    assertProperty(
        slab, "Pset_Edge", "Angle", List.of("45, 30, 15"), "IfcCompoundPlaneAngleMeasure");
    assertProperty(slab, "Pset_Edge", "ListWithNull", List.of("a", "b"), "IfcLabel");
  }

  @Test
  void readsPropertySetDefinitionSetInIfc4() {
    String step =
        ifc4(
            """
            #1=IFCSLAB('0000000000000000000001',$,'Platte',$,$,$,$,$,.FLOOR.);
            #2=IFCPROPERTYSINGLEVALUE('A',$,IFCLABEL('eins'),$);
            #3=IFCPROPERTYSINGLEVALUE('B',$,IFCLABEL('zwei'),$);
            #10=IFCPROPERTYSET('0000000000000000000010',$,'Pset_One',$,(#2));
            #11=IFCPROPERTYSET('0000000000000000000011',$,'Pset_Two',$,(#3));
            #12=IFCRELDEFINESBYPROPERTIES('0000000000000000000012',$,$,$,(#1),IFCPROPERTYSETDEFINITIONSET((#10,#11)));
            """);

    ModelElement slab = parseElements(step).get(0);

    assertProperty(slab, "Pset_One", "A", List.of("eins"), "IfcLabel");
    assertProperty(slab, "Pset_Two", "B", List.of("zwei"), "IfcLabel");
  }

  @Test
  void indexesIfcRelOverridesPropertiesInIfc2x3() {
    String step =
        """
        ISO-10303-21;
        HEADER;
        FILE_DESCRIPTION((''),'2;1');
        FILE_NAME('','2026-11-22T00:00:00',(''),(''),'','','');
        FILE_SCHEMA(('IFC2X3'));
        ENDSEC;
        DATA;
        #1=IFCSLAB('0000000000000000000001',$,'Platte',$,$,$,$,$,.FLOOR.);
        #2=IFCPROPERTYSINGLEVALUE('Flag',$,IFCBOOLEAN(.T.),$);
        #3=IFCPROPERTYSET('0000000000000000000003',$,'Pset_Over',$,(#2));
        #4=IFCRELOVERRIDESPROPERTIES('0000000000000000000004',$,$,$,(#1),#3,(#2));
        ENDSEC;
        END-ISO-10303-21;
        """;

    ModelElement slab = parseElements(step).get(0);

    assertProperty(slab, "Pset_Over", "Flag", List.of("true"), "IfcBoolean");
  }

  @Test
  void handlesCommentsSemicolonsInStringsDerivedComplexEntitiesAndEd3Sections() {
    String step =
        """
        \uFEFF/* leading comment */
        ISO-10303-21;
        HEADER;
        /* multi-line
           header comment with ; inside */
        FILE_DESCRIPTION(('a;b'),'2;1');
        FILE_NAME('name;1','2026-11-22T00:00:00',(''),(''),'','','');
        FILE_SCHEMA(/* inline */ (/* nested */ 'IFC4') /* trailing */);
        ENDSEC;
        ANCHOR;
        <anchor_a>=#1;
        ENDSEC;
        REFERENCE;
        #900=<https://example.org/ref#1>;
        ENDSEC;
        DATA;
        /* skipped entity with ; inside string and comment */
        #1=IFCCARTESIANPOINT((0.,0.,0.));
        #2=IFCSIUNIT(*,.LENGTHUNIT.,$,.METRE.);
        #3=IFCORGANIZATION($,'Revit;Exporter /* not comment */',$,$,$);
        #4=(IFCSIUNIT(*,.LENGTHUNIT.,$,.METRE.) IFCCONVERSIONBASEDUNIT(#2,.LENGTHUNIT.,'ft',#1));
        #10=IFCSLAB($,$,'  Name;with;semicolons  ',*,$,$,$,$,.FLOOR.);
        ENDSEC;
        DATA('second',('IFC4'));
        #20=IFCWALL('0000000000000000000020',$,'Wand',$,$,$,$,$,.STANDARD.);
        ENDSEC;
        SIGNATURE;
        YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXo=/+==
        ENDSEC;
        END-ISO-10303-21;
        """;

    StepModel all = StepParser.parseAll(utf8(step), "complex.ifc");
    // Complex entity #4 is skipped cleanly; #1, #2, #3, #10, #20 are parsed.
    assertThat(all.entities()).extracting(StepEntity::id).containsExactly(1L, 2L, 3L, 10L, 20L);
    assertThat(all.byId(2L).arg(0)).isEqualTo(StepValue.Derived.INSTANCE);
    assertThat(all.byId(3L).text(1)).isEqualTo("Revit;Exporter /* not comment */");

    List<ModelElement> elements =
        IfcModelElements.from(StepParser.parseForElements(utf8(step), "complex.ifc"));
    assertThat(elements).hasSize(2);
    // Missing GlobalId falls back to #id; Name is never trimmed.
    assertThat(elements.get(0).id()).isEqualTo("#10");
    assertThat(elements.get(0).name()).isEqualTo("  Name;with;semicolons  ");
    assertThat(elements.get(1).id()).isEqualTo("0000000000000000000020");
  }

  @Test
  void decodesUtf8AndRawLatin1WithoutReplacementCharacters() {
    String prefix =
        "ISO-10303-21;\nHEADER;\nFILE_SCHEMA(('IFC4'));\nENDSEC;\nDATA;\n"
            + "#1=IFCSLAB('0000000000000000000001',$,'";
    String middle = "',$,$,$,$,$,.FLOOR.);\n#2=IFCSLAB('0000000000000000000002',$,'";
    String suffix = "',$,$,$,$,$,.FLOOR.);\nENDSEC;\nEND-ISO-10303-21;\n";

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.writeBytes(prefix.getBytes(StandardCharsets.US_ASCII));
    out.writeBytes("Brücke UTF-8 Größe".getBytes(StandardCharsets.UTF_8));
    out.writeBytes(middle.getBytes(StandardCharsets.US_ASCII));
    out.writeBytes("Brücke Latin-1 ÄÖÜß".getBytes(StandardCharsets.ISO_8859_1));
    out.writeBytes(suffix.getBytes(StandardCharsets.US_ASCII));

    List<ModelElement> elements =
        IfcModelElements.from(
            StepParser.parseForElements(new ByteArrayInputStream(out.toByteArray()), "mixed.ifc"));

    assertThat(elements)
        .extracting(ModelElement::name)
        .containsExactly("Brücke UTF-8 Größe", "Brücke Latin-1 ÄÖÜß");
  }

  @Test
  void rejectsTruncatedOrCorruptStepFilesNamingSource() {
    String truncatedEntity =
        "ISO-10303-21;\nHEADER;\nFILE_SCHEMA(('IFC4'));\nENDSEC;\nDATA;\n"
            + "#1=IFCSLAB('0000000000000000000001',$,'Platte',$,$,$,$,$,.FLOOR.)";
    String missingData =
        "ISO-10303-21;\nHEADER;\nFILE_SCHEMA(('IFC4'));\nENDSEC;\nEND-ISO-10303-21;\n";
    String missingSchema =
        "ISO-10303-21;\nHEADER;\nFILE_DESCRIPTION((''),'2;1');\nENDSEC;\nDATA;\nENDSEC;\n"
            + "END-ISO-10303-21;\n";
    String unterminatedString =
        "ISO-10303-21;\nHEADER;\nFILE_SCHEMA(('IFC4'));\nENDSEC;\nDATA;\n"
            + "#1=IFCCARTESIANPOINT(('unterminated";
    String missingEndIso =
        "ISO-10303-21;\nHEADER;\nFILE_SCHEMA(('IFC4'));\nENDSEC;\nDATA;\nENDSEC;\n";

    for (String bad :
        List.of(truncatedEntity, missingData, missingSchema, unterminatedString, missingEndIso)) {
      assertThatThrownBy(() -> StepParser.parseForElements(utf8(bad), "kaputt.ifc"))
          .isInstanceOf(ModelLoadException.class)
          .hasMessageContaining("kaputt.ifc");
    }
  }

  @Test
  void rejectsExcessivelyNestedListsAndTypedValuesWithoutStackOverflow() {
    String deepLists = ifc4("#1=IFCSLAB(" + "(".repeat(500) + ")".repeat(500) + ");\n");
    String deepTyped =
        ifc4(
            "#1=IFCPROPERTYSINGLEVALUE('A',$, "
                + "IFCLABEL(".repeat(500)
                + "'x'"
                + ")".repeat(500)
                + ",$);\n");

    for (String deep : List.of(deepLists, deepTyped)) {
      assertThatThrownBy(() -> StepParser.parseForElements(utf8(deep), "tief.ifc"))
          .isInstanceOf(ModelLoadException.class)
          .hasMessageContaining("tief.ifc")
          .hasMessageContaining("verschachtelt");
    }
  }

  @Test
  void readsIfczipSkippingMacOsMetadataAndRejectsMissingOrCorruptZips() throws Exception {
    byte[] validIfc =
        ifc4("#1=IFCSLAB('0000000000000000000001',$,'Platte',$,$,$,$,$,.FLOOR.);\n")
            .getBytes(StandardCharsets.UTF_8);

    Path macZip = folder.resolve("macos.ifczip");
    try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(macZip))) {
      zip.putNextEntry(new ZipEntry("__MACOSX/"));
      zip.closeEntry();
      zip.putNextEntry(new ZipEntry("__MACOSX/._Platte.ifc"));
      zip.write("not-an-ifc".getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
      zip.putNextEntry(new ZipEntry("._Platte.ifc"));
      zip.write("apple-double".getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
      zip.putNextEntry(new ZipEntry("sub/Platte.IFC"));
      zip.write(validIfc);
      zip.closeEntry();
    }

    LoadedModel loaded = LoadedModel.read(macZip.toString());
    assertThat(loaded.schema()).isEqualTo("IFC4");
    assertThat(loaded.elements()).hasSize(1);
    assertThat(loaded.elements().get(0).name()).isEqualTo("Platte");

    Path noIfcZip = folder.resolve("leer.ifczip");
    try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(noIfcZip))) {
      zip.putNextEntry(new ZipEntry("readme.txt"));
      zip.write("hello".getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
    }
    assertThatThrownBy(() -> IfcResources.load(noIfcZip))
        .isInstanceOf(ModelLoadException.class)
        .hasMessageContaining("leer.ifczip")
        .hasMessageContaining(".ifc");

    Path corruptZip =
        Files.write(
            folder.resolve("kaputt.ifczip"),
            new byte[] {'P', 'K', 3, 4, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
    assertThatThrownBy(() -> IfcResources.load(corruptZip))
        .isInstanceOf(ModelLoadException.class)
        .hasMessageContaining("kaputt.ifczip");
  }

  private static String ifc4(String dataLines) {
    return "ISO-10303-21;\n"
        + "HEADER;\n"
        + "FILE_DESCRIPTION((''),'2;1');\n"
        + "FILE_NAME('','2026-11-22T00:00:00',(''),(''),'','','');\n"
        + "FILE_SCHEMA(('IFC4'));\n"
        + "ENDSEC;\n"
        + "DATA;\n"
        + dataLines
        + "ENDSEC;\n"
        + "END-ISO-10303-21;\n";
  }

  private static InputStream utf8(String text) {
    return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
  }

  private static List<ModelElement> parseElements(String step) {
    return IfcModelElements.from(StepParser.parseForElements(utf8(step), "inline.ifc"));
  }

  private static void assertProperty(
      ModelElement element, String pset, String name, List<String> values, String dataType) {
    assertThat(element.find(new PropertyKey(pset, name)))
        .as(pset + "/" + name)
        .isPresent()
        .get()
        .extracting(ModelProperty::values, ModelProperty::dataType)
        .containsExactly(values, dataType);
  }
}
