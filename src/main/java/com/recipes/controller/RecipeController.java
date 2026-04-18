package com.recipes.controller;

import com.recipes.model.Recipe;
import com.recipes.model.User;
import com.recipes.scraper.ScraperService;
import com.recipes.service.XmlService;
import com.recipes.util.ValidationUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class RecipeController {

    private final XmlService xmlService;
    private final ScraperService scraperService;

    public RecipeController(XmlService xmlService, ScraperService scraperService) {
        this.xmlService = xmlService;
        this.scraperService = scraperService;
    }

    @GetMapping("/recipes")
    public ResponseEntity<?> getAllRecipes() {
        try {
            return ResponseEntity.ok(xmlService.getAllRecipes());
        } catch (Exception e) {
            return error(e);
        }
    }

    @GetMapping("/recipes/{id}")
    public ResponseEntity<?> getRecipe(@PathVariable String id) {
        try {
            return xmlService.getRecipeById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return error(e);
        }
    }

    @PostMapping("/recipes")
    public ResponseEntity<?> addRecipe(@RequestBody Map<String, String> body) {
        try {
            String title = body.get("title");
            String cuisine1 = body.get("cuisine1");
            String cuisine2 = body.get("cuisine2");
            String difficulty = body.get("difficulty");

            ValidationUtil.validateRecipe(title, cuisine1, cuisine2, difficulty);

            Recipe r = new Recipe(xmlService.nextRecipeId(), title, cuisine1, cuisine2, difficulty);
            xmlService.addRecipe(r);
            return ResponseEntity.ok(Map.of("message", "Recipe added", "id", r.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return error(e);
        }
    }

    @PostMapping("/users")
    public ResponseEntity<?> addUser(@RequestBody Map<String, String> body) {
        try {
            String name = body.get("name");
            String surname = body.get("surname");
            String skillLevel = body.get("skillLevel");
            String preferredCuisine = body.get("preferredCuisine");

            ValidationUtil.validateUser(name, surname, skillLevel, preferredCuisine);

            User u = new User(xmlService.nextUserId(), name, surname, skillLevel, preferredCuisine);
            xmlService.addUser(u);
            return ResponseEntity.ok(Map.of("message", "User added", "id", u.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return error(e);
        }
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            return ResponseEntity.ok(xmlService.getAllUsers());
        } catch (Exception e) {
            return error(e);
        }
    }

    @GetMapping("/recommend/skill")
    public ResponseEntity<?> recommendBySkill() {
        try {
            Optional<User> userOpt = xmlService.getFirstUser();
            if (userOpt.isEmpty())
                return ResponseEntity.ok(Map.of("recipes", List.of(), "user", null));
            User user = userOpt.get();
            List<Recipe> recipes = xmlService.getRecipesBySkillLevel(user.getSkillLevel());
            return ResponseEntity.ok(Map.of("user", user, "recipes", recipes));
        } catch (Exception e) {
            return error(e);
        }
    }

    @GetMapping("/recommend/skill-and-cuisine")
    public ResponseEntity<?> recommendBySkillAndCuisine() {
        try {
            Optional<User> userOpt = xmlService.getFirstUser();
            if (userOpt.isEmpty())
                return ResponseEntity.ok(Map.of("recipes", List.of(), "user", null));
            User user = userOpt.get();
            List<Recipe> recipes = xmlService.getRecipesBySkillAndCuisine(
                    user.getSkillLevel(), user.getPreferredCuisine());
            return ResponseEntity.ok(Map.of("user", user, "recipes", recipes));
        } catch (Exception e) {
            return error(e);
        }
    }

    @GetMapping(value = "/recipes/xsl-view", produces = "text/html")
    public ResponseEntity<String> xslView(@RequestParam(defaultValue = "") String userId) {
        try {
            String skillLevel = "Beginner";
            if (!userId.isBlank()) {
                List<User> users = xmlService.getAllUsers();
                for (User u : users) {
                    if (u.getId().equals(userId)) {
                        skillLevel = u.getSkillLevel();
                        break;
                    }
                }
            } else {
                Optional<User> first = xmlService.getFirstUser();
                if (first.isPresent())
                    skillLevel = first.get().getSkillLevel();
            }
            String html = xmlService.transformWithXsl(skillLevel);
            return ResponseEntity.ok(html);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("<p>Error: " + e.getMessage() + "</p>");
        }
    }

    @GetMapping("/recipes/by-cuisine")
    public ResponseEntity<?> byCuisine(@RequestParam String cuisine) {
        try {
            return ResponseEntity.ok(xmlService.getRecipesByCuisine(cuisine));
        } catch (Exception e) {
            return error(e);
        }
    }

    @GetMapping("/meta")
    public ResponseEntity<?> getMeta() {
        return ResponseEntity.ok(Map.of(
                "cuisines", xmlService.getCuisines(),
                "difficulties", xmlService.getDifficulties()));
    }

    @PostMapping("/scrape")
    public ResponseEntity<?> scrape(@RequestBody Map<String, String> body) {
        try {
            String url = body.getOrDefault("url",
                    "https://www.bbcgoodfood.com/recipes/collection/budget-autumn");
            List<Recipe> added = scraperService.scrapeAndAdd(url);
            return ResponseEntity.ok(Map.of("added", added.size(), "recipes", added));
        } catch (Exception e) {
            return error(e);
        }
    }

    private ResponseEntity<Map<String, String>> error(Exception e) {
        return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
}
