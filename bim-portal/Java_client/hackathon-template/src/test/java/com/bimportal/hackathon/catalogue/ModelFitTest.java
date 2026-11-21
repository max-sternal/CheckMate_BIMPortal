package com.bimportal.hackathon.catalogue;

import static org.assertj.core.api.Assertions.assertThat;

import com.bimportal.hackathon.catalogue.ModelFit.Fit;
import com.bimportal.hackathon.catalogue.ModelFit.Kind;
import com.bimportal.hackathon.catalogue.ModelFit.Ranked;
import com.bimportal.hackathon.ids.ModelElement;
import com.bimportal.hackathon.ids.ModelElement.ModelProperty;
import com.bimportal.hackathon.ids.PropertyKey;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Ranking catalogue LOINs by how their object type occurs in a model, without any download. */
class ModelFitTest {

  @Test
  void writesUmlautsOutAndDropsCaseAndPunctuation() {
    assertThat(ModelFit.normalize("Fahrbahnübergang")).isEqualTo("fahrbahnuebergang");
    assertThat(ModelFit.normalize("Straße")).isEqualTo("strasse");
    assertThat(ModelFit.normalize("Wand, massiv")).isEqualTo("wandmassiv");
  }

  @Test
  void splitsObjectTypesIntoTheirParts() {
    assertThat(ModelFit.objectTypeWords("Stütze - Pfeiler"))
        .containsExactly("stuetzepfeiler", "stuetze", "pfeiler");
    assertThat(ModelFit.objectTypeWords("Wand, massiv"))
        .containsExactly("wandmassiv", "wand", "massiv");
    assertThat(ModelFit.objectTypeWords("Tür")).containsExactly("tuer");
  }

  @Test
  void readsTheWordsOfANameBeforeTheRepeatedPart() {
    assertThat(ModelFit.nameWords("Kappe_Außen_BW:Kappe_Außen_BW:656578"))
        .containsExactlyInAnyOrder("kappe", "aussen");
    assertThat(ModelFit.nameWords("Fahrbahnübergang_KonstruktionMitDichtprofil"))
        .contains("fahrbahnuebergang", "konstruktion", "dichtprofil", "konstruktionmitdichtprofil")
        .doesNotContain("mit");
  }

  @Test
  void findsTheObjectTypeInTheClassification() {
    ModelFit fit = ModelFit.of(List.of(classified("k1", "Kappe"), classified("k2", "Kappe")));

    Fit kappe = fit.fit(loin("Ueberbau > Kappe (Ingenieurbau): LPH 8-9", "Kappe"));

    assertThat(kappe.kind()).isEqualTo(Kind.CLASSIFICATION);
    assertThat(kappe.elements()).isEqualTo(2);
    assertThat(kappe.describe()).isEqualTo("2 Element(e) mit 'Kappe' in der Klassifizierung");
  }

  @Test
  void comparesWithoutUmlautsAndByParts() {
    ModelFit fit =
        ModelFit.of(List.of(classified("f", "Fahrbahnuebergang"), classified("s", "Stuetze")));

    assertThat(fit.fit(loin("Fahrbahnübergang", "Fahrbahnübergang")).fits()).isTrue();
    assertThat(fit.fit(loin("Stütze - Pfeiler", "Stütze - Pfeiler")).elements()).isEqualTo(1);
  }

  @Test
  void findsTheObjectTypeInANameWhenTheModelIsNotClassified() {
    ModelFit fit =
        ModelFit.of(List.of(element("w1", "Wand-Ext-ERDG-1"), element("w2", "Wand-Int")));

    Fit wand = fit.fit(loin("Wand, massiv", "Wand, massiv"));

    assertThat(wand.kind()).isEqualTo(Kind.NAME);
    assertThat(wand.elements()).isEqualTo(2);
    assertThat(wand.describe()).isEqualTo("2 Element(e) mit 'Wand, massiv' im Namen");
  }

  @Test
  void countsAUserDefinedTypeAsClassification() {
    ModelElement proxy =
        new ModelElement("p", "Proxy", "IfcBuildingElementProxy", "RandKappe", true, List.of());

    assertThat(ModelFit.of(List.of(proxy)).fit(loin("RandKappe LPH 5", "RandKappe")).kind())
        .isEqualTo(Kind.CLASSIFICATION);
  }

  @Test
  void nothingFitsAnObjectTypeTheModelLacks() {
    ModelFit fit = ModelFit.of(List.of(classified("k", "Kappe")));

    assertThat(fit.fit(loin("Fenster", "Fenster"))).isEqualTo(Fit.NONE);
    assertThat(ModelFit.NONE.fit(loin("Ueberbau > Kappe", "Kappe")).fits()).isFalse();
  }

  @Test
  void ranksClassificationFirstThenElementsInTheGroupThenCount() {
    ModelElement lager1 = classified("l1", "Lager", "BeweglichesLager");
    ModelElement lager2 = classified("l2", "Lager", "Ankerplatte");
    ModelElement anker1 = classified("a1", "ErdUndFelsanker", "Ankerplatte");
    ModelElement anker2 = classified("a2", "ErdUndFelsanker", "Ankerplatte");
    ModelElement named = element("p", "Pfeiler_Nord");
    ModelFit fit = ModelFit.of(List.of(lager1, lager2, anker1, anker2, named));

    List<Ranked> ranked =
        fit.rank(
            List.of(
                loin("Pfeiler > Pfeiler (Ingenieurbau): LPH 1-9", "Pfeiler"),
                loin("Fenster", "Fenster"),
                loin("Ueberbau > Ankerplatte (Ingenieurbau): LPH 1-9", "Ankerplatte"),
                loin("Lager > Ankerplatte (Ingenieurbau): LPH 1-9", "Ankerplatte")));

    assertThat(ranked)
        .extracting(r -> r.entry().name())
        .containsExactly(
            // 1 of its 3 Ankerplatte elements is in the group Lager
            "Lager > Ankerplatte (Ingenieurbau): LPH 1-9",
            "Ueberbau > Ankerplatte (Ingenieurbau): LPH 1-9",
            "Pfeiler > Pfeiler (Ingenieurbau): LPH 1-9",
            "Fenster");
    assertThat(ranked.get(0).fit().describe())
        .isEqualTo(
            "3 Element(e) mit 'Ankerplatte' in der Klassifizierung, 1 davon in Gruppe 'Lager'");
  }

  @Test
  void readsTheGroupFromTheName() {
    assertThat(ModelFit.group("Lager > Ankerplatte (Ingenieurbau): LPH 1-9")).isEqualTo("Lager");
    assertThat(ModelFit.group("Stütze - Pfeiler")).isNull();
  }

  /** An element classified the BIM.Hamburg way: group in _IDEbene1, type in _IDEbene3. */
  private static ModelElement classified(String id, String group, String type) {
    return new ModelElement(
        id,
        id,
        "IfcBuildingElementProxy",
        List.of(
            property("_IDEbene1", group),
            property("_IDEbene2", type),
            property("_IDEbene3", type)));
  }

  private static ModelElement classified(String id, String type) {
    return new ModelElement(
        id, id, "IfcBuildingElementProxy", List.of(property("Objekttyp", type)));
  }

  private static ModelElement element(String id, String name) {
    return new ModelElement(id, name, "IfcWall", List.of());
  }

  private static ModelProperty property(String name, String value) {
    return new ModelProperty(new PropertyKey("Pset_Objektinformation", name), value, "IfcText");
  }

  static CatalogueEntry loin(String name, String objectType) {
    return new CatalogueEntry(
        UUID.nameUUIDFromBytes(name.getBytes()),
        name,
        "1.0",
        "Test",
        List.of(objectType),
        List.of("Ingenieurbau"),
        List.of(),
        List.of("LPH 5: Ausführungsplanung"),
        List.of());
  }
}
