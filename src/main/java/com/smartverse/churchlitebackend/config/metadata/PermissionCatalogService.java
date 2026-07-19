package com.smartverse.churchlitebackend.config.metadata;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartverse.churchlitebackend_gen.dtos.PermissionResourceDTO;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PermissionCatalogService {

 private final ObjectMapper mapper;
 private final List<PermissionResourceDTO> resources;

 public PermissionCatalogService(ObjectMapper mapper) throws IOException {
    this.mapper = mapper;
    List<Map<String, Object>> raw = mapper.readValue(
            new ClassPathResource("resources.json").getInputStream(),
            new TypeReference<>() {
            }
    );

    Map<String, List<String>> result = new LinkedHashMap<>();
    Map<String, String> descriptions = new LinkedHashMap<>();

    for (var item : raw) {
     if (item.get("premissions") instanceof List<?> values) {
      String name = String.valueOf(item.get("resource"));
      descriptions.putIfAbsent(name, String.valueOf(item.getOrDefault("description", name)));

      if (
              values.isEmpty()
                      || name.equals("permissionResource")
                      || name.equals("permissionGroupMember")
                      || name.equals("permissionGroupDenial")
      ) {
       continue;
      }

      result.computeIfAbsent(name, key -> new ArrayList<>())
              .addAll(
                      values.stream()
                              .map(String::valueOf)
                              .map(String::toUpperCase)
                              .toList()
              );
     }
    }

    result.put(
            "permissionGroup",
            List.of("CREATE", "VIEW", "UPDATE", "DELETE")
    );

    resources = result.entrySet()
            .stream()
            .map(entry -> new PermissionResourceDTO(
                    null,
                    entry.getKey(),
                    descriptions.getOrDefault(entry.getKey(), entry.getKey()),
                    entry.getValue()
                            .stream()
                            .distinct()
                            .toList()
            ))
            .toList();
 }

 public List<PermissionResourceDTO> resources() {
    try {
     return new PermissionCatalogService(mapper).resources;
    } catch (IOException exception) {
     throw new IllegalStateException("Unable to load permission catalog from resources.json", exception);
    }
 }

 public String resolvePermission(String resource, String method) {
  var item = resources().stream()
          .filter(permissionResource ->
                  permissionResource.getResource().equals(resource)
          )
          .findFirst();

  if (item.isEmpty()) {
   return null;
  }

  String action = switch (method) {
   case "POST" -> "CREATE";
   case "PUT", "PATCH" -> "UPDATE";
   case "DELETE" -> "DELETE";
   case "GET" -> "VIEW";
   default -> null;
  };

  if (
          action != null
                  && item.get().getPermissions().contains(action)
  ) {
   return action;
  }

  return item.get().getPermissions().size() == 1
          ? item.get().getPermissions().getFirst()
          : action;
 }

 public String resolveResource(String uri, String contextPath) {
  int prefix = contextPath == null
          ? 0
          : contextPath.length();

  String first = uri
          .substring(Math.min(uri.length(), prefix))
          .replaceFirst("^/+", "")
          .split("/", 2)[0];

  if (first.equals("getPermissionResources")) {
   return "permissionGroup";
  }

  return resources().stream()
          .map(PermissionResourceDTO::getResource)
          .filter(first::equals)
          .findFirst()
          .orElse(null);
 }
}
