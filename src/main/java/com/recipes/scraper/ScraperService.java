package com.recipes.scraper;

import com.recipes.model.Recipe;
import com.recipes.service.XmlService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ScraperService {

    private final XmlService xmlService;

    public ScraperService(XmlService xmlService) {
        this.xmlService = xmlService;
    }

    public List<Recipe> scrapeAndAdd(String url) throws Exception {
        Document page = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (compatible; RecipeBot/1.0)")
                .timeout(15_000)
                .get();

        // recipe cards have an <a><h2></a> as a recipe title
        Elements cards = page.select("h2.heading-4, h3.heading-4, [data-testid='recipe-card-title']");
        if (cards.isEmpty()) {
            // any linked heading inside a card
            cards = page.select("a[href*='/recipes/'] h2, a[href*='/recipes/'] h3");
        }

        List<Recipe> added = new ArrayList<>();
        for (Element card : cards) {
            String title = card.text().trim();
            if (title.isBlank())
                continue;

            Recipe r = new Recipe(
                    xmlService.nextRecipeId(),
                    title,
                    xmlService.randomCuisine(),
                    xmlService.randomCuisine(),
                    xmlService.randomDifficulty());
            xmlService.addRecipe(r);
            added.add(r);
        }
        return added;
    }
}
