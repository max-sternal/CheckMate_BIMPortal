package com.bimportal.hackathon.examples;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bimportal.client.model.ClassificationPublicDto;
import com.bimportal.client.model.LOINPublicDto;
import com.bimportal.client.model.PropertyOrGroupForPublicDto;
import com.bimportal.client.model.PropertyOrGroupForPublicRequest;
import com.bimportal.client.model.PropertyOrGroupPublicReference;
import com.bimportal.client.model.SimpleAiaProjectPublicDto;
import com.bimportal.client.model.SimpleLoinPublicDto;
import com.pb40.bimportal.client.BimPortalClientBuilder;
import com.pb40.bimportal.client.EnhancedBimPortalClient;

/**
 * Basic example demonstrating core BIM Portal API functionality.
 *
 * <p>
 * This example shows how to: - Authenticate with the API - Search for projects
 * and LOINs - Retrieve detailed information - Perform basic exports
 */
public class BasicExample {

	private static final Logger logger = LoggerFactory
			.getLogger(BasicExample.class);

	public static void main(String[] args) {

		loadLoinByName("Schaft");
	}

	public static String loadLoinByName(String name) {
		try {
			// Initialize client
			EnhancedBimPortalClient client = BimPortalClientBuilder
					.buildDefault();

			client.searchProjects();
			String r = loinSearch(client, name);

			client.logout();
			System.out.println("✅ Basic example completed successfully!");

			return r;

		} catch (Exception e) {
			logger.error("Error in basic example", e);
			System.err.println("❌ Error: " + e.getMessage());
		}
		return "ERRRO";
	}

	public static String loinSearch(EnhancedBimPortalClient client,
			String filterType) {
		List<SimpleLoinPublicDto> loins = client.searchLoins();

		// System.out.println("Found " + loins.size() + " domain models:");

		StringBuilder sb = new StringBuilder();

		for (SimpleLoinPublicDto loin : loins) {
			// System.out.println(
			// " - " + loin.getName() + " (" + loin.getGuid() + ")");

			if (loin.getName().contains(filterType) == false) {
				continue;

			}

			Optional<LOINPublicDto> opLOIN = client.getLoin(loin.getGuid());

			if (opLOIN.isPresent()) {
				LOINPublicDto loinDetail = opLOIN.get();

				sb.append("\n");
				sb.append(loinDetail.getName());
				sb.append(" (" + loinDetail.getGuid() + ")\n");
				sb.append(" - Description: " + loinDetail.getDescription()
						+ "\n");
				sb.append(
						" - Version: " + loinDetail.getVersionNumber() + "\n");
				sb.append(" - Created: " + loinDetail.getCreatedDate() + "\n");

				Set<PropertyOrGroupPublicReference> objectTypes = loinDetail
						.getObjectTypes();

				sb.append("\n\nClassifications:\n");
				for (ClassificationPublicDto classif : loinDetail
						.getClassifications()) {

					sb.append("Classification: \n"
							+ classif.getProperty().getName() + " ("
							+ classif.getPropertyGroup().getName() + ")\n");

					System.out.println(" - Classification: "
							+ classif.getProperty().getName() + " ("
							+ classif.getPropertyGroup().getName() + ")");
				}

				sb.append("\n\nObject Types:\n");
				for (PropertyOrGroupPublicReference type : loinDetail
						.getObjectTypes()) {

					sb.append("Object Type: \n" + type.getName() + " ("
							+ type.getGuid() + ")" + "\n");

					System.out.println(" - Object Type: " + type.getName()
							+ " (" + type.getGuid() + ")");
				}

				sb.append("\n\nMerkmals-Gruppen:\n");
				for (PropertyOrGroupPublicReference prop : loinDetail
						.getProperties()) {

					sb.append("\n Merkmals Gruppen: \n - Merkmals-Gruppe: "
							+ prop.getName() + " (" + prop.getGuid() + ")"
							+ "\n");
					System.out.println(" - Merkmals-Gruppe: " + prop.getName()
							+ " (" + prop.getGuid() + ")");

					// PropertyOrGroupForPublicRequest propertyRequest1 = new
					// PropertyOrGroupForPublicRequest();
					//
					// propertyRequest1.addFilterGuidsItem(prop.getGuid());
					// List<PropertyOrGroupForPublicDto> properties1 = client
					// .searchProperties(propertyRequest1);
					//
					// properties.forEach(p -> {
					// System.out.println("Property: " + p.getName() + " ("
					// + p.getGuid() + ")");
					//
					// });
					//
					// // Check if prop is a PropertyGroupDto
					// Optional<PropertyGroupDto> groupOpt = client
					// .getPropertyGroup(prop.getGuid());
					// if (groupOpt.isPresent()) {
					// // PropertyGroupDto group = groupOpt.get();
					// // System.out.println(" - Property Group: " +
					// // group.getGuid());
					// // group.getProperties().forEach(p -> {
					// // System.out.println(" - Property: "
					// // + " (" + p.getGuid() + ")");
					// }
				}
			}

			else {
				System.out
						.println("LOIN not found for GUID: " + loin.getGuid());
			}
		}

		System.out.println("LOIN FOUND");

		return sb.toString();
	}

	public static void runLoinExportExamples() {
		try {
			// Initialize client
			EnhancedBimPortalClient client = BimPortalClientBuilder
					.buildDefault();

			runLoinExportExamples(client);

			// Clean up
			client.logout();
			System.out
					.println("✅ LOIN export examples completed successfully!");

		} catch (Exception e) {
			logger.error("Error in LOIN export examples", e);
			System.err.println("❌ Error: " + e.getMessage());
		}
	}

	public static void runLoinExportExamples(EnhancedBimPortalClient client) {
		System.out.println(
				"\n============================================================");
		System.out.println("📋 LOIN EXPORT EXAMPLES");
		System.out.println(
				"============================================================");

		System.out.println("\n1️⃣ Searching for available LOINs...");
		try {
			List<SimpleLoinPublicDto> loins = client.searchLoins();
			if (loins.isEmpty()) {
				System.out.println("🔭 No LOINs found for export");
				return;
			}

			System.out.println("✅ Found " + loins.size() + " LOINs:");
			for (int i = 0; i < Math.min(3, loins.size()); i++) {
				SimpleLoinPublicDto loin = loins.get(i);

				loin.getObjectTypes().forEach(type -> {
					System.out.println("      - Object Type: " + type);

				});

				PropertyOrGroupForPublicRequest propertyRequest = new PropertyOrGroupForPublicRequest();

				List<PropertyOrGroupForPublicDto> properties = client
						.searchProperties(propertyRequest);

				// Call the API with parent ID
				for (UUID parentGuid : properties.getFirst().getParentGuids()) {
					var propetyGroups = client.getPropertyGroup(parentGuid);
					// System.out.println("Property Group: " + propetyGroups);
				}

				System.out.println("Found " + properties.size()
						+ " properties matching " + loin.getGuid());

				System.out.println("   " + (i + 1) + ". " + loin.getName()
						+ " (" + loin.getGuid() + ")");
			}

			SimpleLoinPublicDto selectedLoin = loins.get(0);
			System.out.println(
					"\n🎯 Using LOIN: '" + selectedLoin.getName() + "'");

		} catch (Exception e) {
			logger.error("Error searching for LOINS", e);
			System.err
					.println("❌ Error searching for LOINS: " + e.getMessage());
		}
	}

	public static void projectSearch(EnhancedBimPortalClient client) {
		System.out.println("\n📋 Project Search Example");
		System.out.println("--------------------------");

		try {
			// Search all projects
			List<SimpleAiaProjectPublicDto> projects = client.searchProjects();

			System.out.println("Found " + projects.size() + " projects");

			// Display first few projects
			for (int i = 0; i < Math.min(3, projects.size()); i++) {
				SimpleAiaProjectPublicDto project = projects.get(i);
				System.out.println((i + 1) + ". " + project.getName());
				System.out.println("   GUID: " + project.getGuid());
				System.out.println(
						"   Description: " + (project.getDescription() != null
								? project.getDescription()
								: "N/A"));
			}
			// todo: return loin guid instead
		} catch (Exception e) {
			logger.error("Error in project search", e);
			System.err.println("❌ Project search error: " + e.getMessage());
		}
	}

	public static void readLoins(List<SimpleLoinPublicDto> loins,
			EnhancedBimPortalClient client) {
		Map<String, List<String[]>> classifications = new HashMap<>();
		Map<String, List<String>> merkmalsgruppen = new HashMap<String, List<String>>();
		for (SimpleLoinPublicDto loin : loins) {

			Optional<LOINPublicDto> opLOIN = client.getLoin(loin.getGuid());

			if (opLOIN.isPresent()) {
				LOINPublicDto loinDetail = opLOIN.get();

				// System.out.println(loinDetail);

				for (ClassificationPublicDto cla : loinDetail
						.getClassifications()) {

					if (classifications.containsKey(loin.getName())) {
						List<String[]> ebenen = classifications
								.get(loin.getName());
						ebenen.add(new String[]{cla.getProperty().getName(),
								cla.getPropertyGroup().getName()});
					} else {
						List<String[]> e = new ArrayList<String[]>();
						e.add(new String[]{cla.getProperty().getName(),
								cla.getPropertyGroup().getName()});
						classifications.put(loin.getName(), e);
					}

				}

				List<String> psets = new ArrayList<String>();
				for (PropertyOrGroupPublicReference p : loinDetail
						.getProperties()) {

					psets.add(p.getName());

				}

				merkmalsgruppen.put(loinDetail.getName(), psets);

				// loinDetail.getClassifications().forEach(classif -> {
				// System.out.println(" - Classification: " + classif);
				// });
				//
				// loinDetail.getObjectTypes().forEach(type -> {
				// System.out.println(" - Object Type: " + type);
				//
				// });

				// for (PropertyOrGroupPublicReference p : loinDetail
				// .getProperties()) {
				//
				// client.getPropertyGroupsApi()
				// .getPropertyGroupForPublic(p.getGuid());
				//
				// }

				// PropertyOrGroupForPublicRequest propertyRequest2 = new
				// PropertyOrGroupForPublicRequest();
				//
				// propertyRequest2.addFilterGuidsItem(loin.getGuid());
				// List<PropertyOrGroupForPublicDto> group1 = client
				// .searchPropertyGroups(propertyRequest2);
				//
				// System.out.println("Group 1: " + group1.toString());
				//
				// loinDetail.getProperties().forEach(prop -> {
				//
				// System.out.println(" - Merkmals-Gruppe: "
				// + prop.getName() + " (" + prop.getGuid() + ")");
				//
				// System.out.println(prop);
				//
				// // client.searchProperties();
				//
				// // Check if prop is a PropertyGroupDto
				// client.getPropertyGroup(prop.getGuid()).ifPresent(group -> {
				// System.out.println(
				// " - Property Group: " + group.getGuid());
				// group.getProperties().forEach(p -> {
				// System.out.println(" - Property: " + " ("
				// + p.getGuid() + ")");
				// });
				// });
				//
				// });

				// System.out.println("LOIN Detail: " + loinDetail);
			} else {
				System.out
						.println("LOIN not found for GUID: " + loin.getGuid());
			}

			// System.out.println("SEARCH: " + loin.getGuid());
			// propertySearch(client, loin.getGuid());

		}

		// Ausgabe der Classifications Map
		System.out.println("=== CLASSIFICATIONS ===");
		System.out.println("Anzahl LOIN-Elemente: " + classifications.size());
		System.out.println();

		for (Map.Entry<String, List<String[]>> entry : classifications
				.entrySet()) {
			String loinName = entry.getKey();
			List<String[]> classificationList = entry.getValue();

			System.out.println("LOIN: " + loinName);
			System.out.println(
					"  Anzahl Classifications: " + classificationList.size());

			for (int i = 0; i < classificationList.size(); i++) {
				String[] classification = classificationList.get(i);
				System.out.println("  [" + (i + 1) + "] Property: '"
						+ classification[0] + "' | PropertyGroup: '"
						+ classification[1] + "'");
			}
			System.out.println();
		}

		System.out.println("\n" + "=".repeat(50) + "\n");

		// Ausgabe der Merkmalsgruppen Map
		System.out.println("=== MERKMALSGRUPPEN ===");
		System.out.println("Anzahl LOIN-Elemente: " + merkmalsgruppen.size());
		System.out.println();

		for (Map.Entry<String, List<String>> entry : merkmalsgruppen
				.entrySet()) {
			String loinName = entry.getKey();
			List<String> properties = entry.getValue();

			System.out.println("LOIN: " + loinName);
			System.out.println("  Anzahl Properties: " + properties.size());

			for (int i = 0; i < properties.size(); i++) {
				System.out.println("  [" + (i + 1) + "] " + properties.get(i));
			}
			System.out.println();
		}

	}

	public static List<PropertyOrGroupForPublicDto> propertySearch(
			EnhancedBimPortalClient client, UUID aiaProjectGuid) {
		System.out.println("\n🏷 Property Search Example");
		System.out.println("---------------------------");

		System.out.println(
				"Searching properties for LOIN GUID: " + aiaProjectGuid);

		// var loinResponse = client.getLoin(
		// UUID.fromString("18463087-4cb3-4be4-8c94-e578169cefaf"));
		// System.out.println("LOIN: " + loinResponse);
		// System.out.println("---------------------------");

		PropertyOrGroupForPublicRequest propertyRequest = new PropertyOrGroupForPublicRequest();

		propertyRequest.addFilterGuidsItem(aiaProjectGuid);
		propertyRequest.addOrganisationGuidsItem(aiaProjectGuid);
		// propertyRequest.searchString("Schaft LOI 500");

		System.out.println(propertyRequest.toString());

		List<PropertyOrGroupForPublicDto> properties = client
				.searchPropertyGroups(propertyRequest);

		PropertyOrGroupForPublicRequest propertyRequest2 = new PropertyOrGroupForPublicRequest();
		// propertyRequest2.addFilterGuidsItem(aiaProjectGuid);
		propertyRequest2.filterGuids(List.of(aiaProjectGuid));
		List<PropertyOrGroupForPublicDto> properties2 = client
				.searchPropertyGroups(propertyRequest2);

		properties2.forEach(prop -> {
			System.out.println("Property: " + prop.getName() + " ("
					+ prop.getGuid() + ") " + prop.getParentGuids().size());
		});

		//
		// // Call the API with parent ID
		// for (UUID parentGuid : properties.getFirst().getParentGuids()) {
		// var propetyGroups = client.getPropertyGroup(parentGuid);
		// System.out.println("Property Group: " + propetyGroups);
		// }

		System.out.println("Found " + properties.size()
				+ " properties matching " + aiaProjectGuid);

		return properties;
	}

}