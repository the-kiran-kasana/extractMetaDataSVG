
package extract.meta_data;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Controller
public class FileUploadController {

    private final ResourceLoader resourceLoader;

    public FileUploadController(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @RequestMapping("/")
    public String index() {
        return "index";
    }

    @RequestMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file, final Model model) {
        try {
            final Document doc = Jsoup.parse(file.getInputStream(), "UTF-8", "");
            final Element svgElement = doc.select("svg").first();
            final String filePath = resourceLoader.getResource("classpath:/templates/output.json").getFile().getAbsolutePath();

            final Map<String, Map<String, String>> cssClassesToPropertiesMap = buildCSSClassesToPropertiesMap(doc.select("style"));

            if (!Objects.isNull(svgElement)) {
                final Element filteredSvgElement = removeImageNodes(svgElement);

                String outPut = convertSVGtoJsonArray(filteredSvgElement, cssClassesToPropertiesMap);
                final String finalString = "{ \"text_fields\": [" + outPut + "] }";

                saveToRTF(finalString, filePath);
                model.addAttribute("absoluteFilePath", filePath);

                return "uploaded";
            } else {
                return "No SVG content found.";
            }
        } catch (final IOException e) {
            return "Exception occurred while processing SVG file. Exception : " + e.getMessage();
        }
    }

    private Map<String, Map<String, String>> buildCSSClassesToPropertiesMap(final Elements elements) {
        for (Element style : elements) {
            String cssText = style.html();
            //System.out.println("CSS Properties: " + cssText);
            Map<String, Map<String, String>> classToPropertiesMap = getCssPropertiesForClass(cssText);
            //System.out.println("classToPropertiesMap : " + classToPropertiesMap);
            return classToPropertiesMap;
        }
        // If no Style found, return empty map.
        return new HashMap<>();
    }






    private static Map<String, Map<String, String>> getCssPropertiesForClass(String cssText) {
        // Split the CSS text by lines
        String[] lines = cssText.split("\\r?\\n");
        final Map<String, Map<String, String>> classToMapOfProperties = new HashMap<>();
        Map<String, String> properties = null;
        String currentClass = null;
    
        // Iterate over each line
        for (String line : lines) {
            // Trim leading and trailing whitespace
            line = line.trim();
    
            // Check if the line is not empty
            if (!line.isEmpty()) {
                // Check if the line contains a CSS class definition
                if (line.startsWith(".") && line.contains("{")) {
                    // Extract class name
                    int index = line.indexOf("{");
                    currentClass = line.substring(0, index).trim();
                    // Remove the leading dot
                    currentClass = currentClass.substring(1);
    
                    // Initialize properties map for the current class
                    properties = new HashMap<>();
                    classToMapOfProperties.put(currentClass, properties);
                    
                    // Extract properties if the class is in multiple lines
                    String propertiesText = line.substring(index + 1);
                    propertiesText = propertiesText.replace("{", "").trim();
                    if (!propertiesText.isEmpty()) {
                        // Split the propertiesText by semicolon to separate properties
                        String[] propertiesArray = propertiesText.split(";");
                        // Iterate over each property
                        for (String property : propertiesArray) {
                            // Split the property by colon to separate property name and value
                            String[] propertyParts = property.split(":", 2);
                            if (propertyParts.length == 2) {
                                String propertyName = propertyParts[0].trim();
                                String propertyValue = propertyParts[1].trim();
                                // Store the property in the map
                                properties.put(propertyName, propertyValue);
                            }
                        }
                    }
                } else if (line.contains(":") && properties != null) { // Check if the line contains a CSS property
                    // Split the property by colon to separate property name and value
                    String[] propertyParts = line.split(":", 2);
                    if (propertyParts.length == 2) {
                        String propertyName = propertyParts[0].trim();
                        String propertyValue = propertyParts[1].trim();
                        // Store the property in the map
                        properties.put(propertyName, propertyValue);
                    }
                }
            }
        }
        return classToMapOfProperties;
    }
    












    // private static Map<String, Map<String, String>> getCssPropertiesForClass(String cssText) {
    //     // Split the CSS text by lines
    //     String[] lines = cssText.split("\\r?\\n");
    //     final Map<String, Map<String, String>> classToMapOfProperties = new HashMap<>();
    
    //     // Initialize a map to store CSS properties for the current class
    //     Map<String, String> properties = null;
    
    //     // Iterate over each line
    //     for (String line : lines) {
    //         // Trim leading and trailing whitespace
    //         line = line.trim();
    
    //         // Check if the line is not empty
    //         if (!line.isEmpty()) {
    //             // Check if the line contains a CSS class definition
    //             if (line.startsWith(".")) {
    //                 // Extract class name
    //                 String className = line.substring(0, line.indexOf('{')).trim();
    //                 // Remove the leading dot
    //                 className = className.substring(1);
    
    //                 // Initialize properties map for the current class
    //                 properties = new HashMap<>();
    //                 classToMapOfProperties.put(className, properties);
    //             } else if (line.contains(":")) { // Check if the line contains a CSS property
    //                 // Split the property by colon to separate property name and value
    //                 String[] propertyParts = line.split(":", 2);
    //                 if (propertyParts.length == 2 && properties != null) {
    //                     String propertyName = propertyParts[0].trim();
    //                     String propertyValue = propertyParts[1].trim();
    //                     // Store the property in the map
    //                     properties.put(propertyName, propertyValue);
    //                 }
    //             }
    //         }
    //     }
    
    //     return classToMapOfProperties;
    // }


    // private static Map<String, Map<String, String>> getCssPropertiesForClass(String cssText) {
    //     // Split the CSS text by lines
    //     String[] lines = cssText.split("\\r?\\n");
    //     final Map<String, Map<String, String>> classToMapOfProperties = new HashMap<>();

    //     // Iterate over each line
    //     for (String line : lines) {
            
    //         // Initialize a map to store CSS properties
    //         Map<String, String> properties = new HashMap<>();

    //         // Trim leading and trailing whitespace
    //         line = line.trim();
            
    //         // Split the line by opening brace to separate class and properties
    //         String[] parts = line.split("\\{", 2);
    //         if (parts.length == 2) {
    //             String propertiesClass = parts[0];
    //             propertiesClass = propertiesClass.replace(".", "").trim();

    //             String propertiesText = parts[1];
    //             // Remove the closing brace from propertiesText
    //             propertiesText = propertiesText.replace("}", "").trim();
    //             // Split the propertiesText by semicolon to separate properties
    //             String[] propertiesArray = propertiesText.split(";");
    //             // Iterate over each property
    //             for (String property : propertiesArray) {
    //                 // Split the property by colon to separate property name and value
    //                 String[] propertyParts = property.split(":", 2);
    //                 if (propertyParts.length == 2) {
    //                     String propertyName = propertyParts[0].trim();
    //                     String propertyValue = propertyParts[1].trim();
    //                     // Store the property in the map
    //                     properties.put(propertyName, propertyValue);
    //                 }
    //             }
    //             // Class found, return properties
    //             classToMapOfProperties.put(propertiesClass, properties);
    //         }
    //     }
    //     return classToMapOfProperties;
    // }










    private Element removeImageNodes(Element svgElement) {
        // Select image elements within the svg
        Elements imageElements = svgElement.select("image");
        for (Element imageElement : imageElements) {
            imageElement.remove();
        }
        return svgElement;
    }

    private void saveToRTF(final String fileContent, final String fileAbsolutePath) {
        try (final FileOutputStream fos = new FileOutputStream(fileAbsolutePath);
             final OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
             final BufferedWriter writer = new BufferedWriter(osw)) {
            writer.write(fileContent);

        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private Map<String, String> getAllPropertiesForClasses(final Map<String, Map<String, String>> classesToPropertiesMap, final String classNames) {
        final List<String> classNameList = Arrays.asList(classNames.split(" "));
        Map<String, String> allProperties = new HashMap<>();
        for(String classId : classNameList) {
            final String trimmedClassId = classId.trim();
            System.out.println(trimmedClassId);
            if(classesToPropertiesMap.containsKey(trimmedClassId)) {
                for (Map.Entry<String, String> entry : classesToPropertiesMap.get(trimmedClassId).entrySet()) {
                    allProperties.put(entry.getKey(), entry.getValue());
                    System.out.println(entry.getKey());
                    System.out.println(entry.getValue());

                }
            }
        }
        return allProperties;
    }

    private String getAttributeName(final String orgAttrName) {
        if (orgAttrName.equalsIgnoreCase("x")) {
            return "left";
        }
        if (orgAttrName.equalsIgnoreCase("y")) {
            return "top";
        }
        return orgAttrName;
    }





    private String convertSVGtoJsonArray(Element element, final Map<String, Map<String, String>> classesToPropertiesMap) {
        StringBuilder jsonArrayBuilder = new StringBuilder();
        boolean isFirstElement = true;
    
        if ("text".equals(element.tagName())) {
            JSONObject jsonNode = new JSONObject();
            Attributes svgAttributes = element.attributes();
    
            jsonNode.put("tag_type", element.tagName());
            jsonNode.put("tag_name", element.tagName());
    
            for (Attribute attr : svgAttributes) {
                if ("class".equals(attr.getKey())) {
                    for (Map.Entry<String, String> entry : getAllPropertiesForClasses(classesToPropertiesMap, attr.getValue()).entrySet()) {
                        jsonNode.put(entry.getKey(), entry.getValue());
                    }
                } else if ("transform".equals(attr.getKey())) {
                    String transformValue = attr.getValue();
                    if (transformValue.startsWith("matrix")) {
                        // Parse the matrix and extract translation values
                        String[] matrixValues = transformValue.substring(transformValue.indexOf("(") + 1, transformValue.indexOf(")")).split(" ");
                        double translateX = Double.parseDouble(matrixValues[4].replaceAll(",", ""));
                        double translateY = Double.parseDouble(matrixValues[5].replaceAll(",", ""));


                        String leftValue = String.format("%.0f", translateX);
                        String topValue = String.format("%.0f", translateY);

                        // Append the decimal part to the JSON key
                        jsonNode.put("left", leftValue.substring(leftValue.indexOf('.') + 1)+"px");
                        jsonNode.put("top", topValue.substring(topValue.indexOf('.') + 1)+"px");
                            
                    }
                } else {
                    final String attrFinalName = getAttributeName(attr.getKey());
                    jsonNode.put(attrFinalName, attr.getValue());
                }
            }
    
            String textContent = element.text();
            jsonNode.put("text_content", textContent);
    
            // Append the JSON object to the JSON array builder
            if (!isFirstElement) {
                jsonArrayBuilder.append(",");
            } else {
                isFirstElement = false;
            }
            jsonArrayBuilder.append(jsonNode.toString());
        }
    
        for (Node child : element.childNodesCopy()) {
            if (child instanceof Element) {
                String returnedString = convertSVGtoJsonArray((Element) child, classesToPropertiesMap);
                if (!(returnedString == null || "".equals(returnedString))) {
                    if (!isFirstElement) {
                        jsonArrayBuilder.append(",");
                    } else {
                        isFirstElement = false;
                    }
                    jsonArrayBuilder.append(returnedString);
                }
            }
        }
    
        return  jsonArrayBuilder.toString();
    }
}