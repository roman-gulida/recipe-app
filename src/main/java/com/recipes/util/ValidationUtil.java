package com.recipes.util;

import java.util.Arrays;
import java.util.List;

public class ValidationUtil {

    private static final List<String> VALID_CUISINES = Arrays.asList(
        "Italian", "Asian", "Mexican", "French", "British",
        "Mediterranean", "American", "Indian", "Middle Eastern", "Japanese"
    );
    private static final List<String> VALID_DIFFICULTIES = Arrays.asList(
        "Beginner", "Intermediate", "Advanced"
    );

    public static void validateRecipe(String title, String cuisine1, String cuisine2, String difficulty) {
        if (title == null || title.isBlank())
            throw new IllegalArgumentException("Title is required.");
        if (!VALID_CUISINES.contains(cuisine1))
            throw new IllegalArgumentException("Invalid cuisine1: " + cuisine1);
        if (!VALID_CUISINES.contains(cuisine2))
            throw new IllegalArgumentException("Invalid cuisine2: " + cuisine2);
        if (!VALID_DIFFICULTIES.contains(difficulty))
            throw new IllegalArgumentException("Invalid difficulty: " + difficulty);
    }

    public static void validateUser(String name, String surname, String skillLevel, String preferredCuisine) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Name is required.");
        if (surname == null || surname.isBlank())
            throw new IllegalArgumentException("Surname is required.");
        if (!VALID_DIFFICULTIES.contains(skillLevel))
            throw new IllegalArgumentException("Invalid skill level: " + skillLevel);
        if (!VALID_CUISINES.contains(preferredCuisine))
            throw new IllegalArgumentException("Invalid preferred cuisine: " + preferredCuisine);
    }
}
