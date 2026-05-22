package com.example.bidmart.user.config;

import com.example.bidmart.common.security.PermissionNames;
import com.example.bidmart.user.model.Permission;
import com.example.bidmart.user.repository.PermissionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class PermissionSeeder implements CommandLineRunner {

    private final PermissionRepository permissionRepository;

    public PermissionSeeder(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Override
    public void run(String... args) {
        Set<String> existing = new HashSet<>();
        for (Permission permission : permissionRepository.findAll()) {
            existing.add(permission.getName());
        }

        List<Permission> toSave = new ArrayList<>();
        for (String name : loadPermissionNames()) {
            if (!existing.contains(name)) {
                Permission permission = new Permission();
                permission.setName(name);
                toSave.add(permission);
            }
        }

        if (!toSave.isEmpty()) {
            permissionRepository.saveAll(toSave);
        }
    }

    private List<String> loadPermissionNames() {
        List<String> names = new ArrayList<>();
        for (Field field : PermissionNames.class.getDeclaredFields()) {
            int mods = field.getModifiers();
            if (Modifier.isPublic(mods) && Modifier.isStatic(mods) && field.getType() == String.class) {
                try {
                    String name = (String) field.get(null);
                    if (name != null && !name.isBlank()) {
                        names.add(name);
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Failed to read permission names.", e);
                }
            }
        }
        return names;
    }
}
