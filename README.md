# Recipe Recommender - Spring Boot + XML & XSL

## Requirements Coverage

| # | Requirement                                    | Implementation                                      |
|---|------------------------------------------------|-----------------------------------------------------|
| 1 | 20+ recipes in XML (scraping from BBC Good Food) | `recipes.xml` pre-seeded; `ScraperService.java` scrapes at runtime via `/api/scrape` |
| 2 | DTD/XSD validation                             | `recipes.xsd` (XSD schema with enumerations)       |
| 3 | Read XML into memory & display in UI           | `XmlService.getAllRecipes()` > `GET /api/recipes` > card grid |
| 4 | Add recipe form + XML save + validation        | `POST /api/recipes` > `ValidationUtil` > DOM append + file save |
| 5 | Add user form + XML save                       | `POST /api/users` > DOM append + file save         |
| 6 | Recommend by skill level (first user, XPath)   | `GET /api/recommend/skill` > XPath `//recipe[difficulty='X']` |
| 7 | Recommend by skill + cuisine (first user, XPath)| `GET /api/recommend/skill-and-cuisine` > XPath with both conditions |
| 8 | XSL view with yellow/green row colouring       | `GET /api/recipes/xsl-view` > Saxon XSLT > `recipes.xsl` |
| 9 | Single recipe detail (XPath)                   | `GET /api/recipes/{id}` > XPath `//recipe[@id='X']` |
|10 | Browse by cuisine type (XPath)                 | `GET /api/recipes/by-cuisine?cuisine=X` > XPath `cuisine1 or cuisine2` |
|11 | Intuitive UI                                   | Sidebar nav, cards, modal, user selector for XSL   |

---


The app copies `recipes.xml` from the classpath into `./data/recipes.xml` on first start. All writes (new recipes/users) go there.

---

## API Reference

| Method | Endpoint                            | Description                            |
|--------|-------------------------------------|----------------------------------------|
| GET    | `/api/recipes`                      | All recipes                  |
| GET    | `/api/recipes/{id}`                 | Single recipe detail           |
| POST   | `/api/recipes`                      | Add recipe                    |
| GET    | `/api/recipes/xsl-view?userId=`     | XSLT-transformed HTML table    |
| GET    | `/api/recipes/by-cuisine?cuisine=`  | Filter by cuisine              |
| GET    | `/api/users`                        | All users                              |
| POST   | `/api/users`                        | Add user                      |
| GET    | `/api/recommend/skill`              | Recommend by skill level       |
| GET    | `/api/recommend/skill-and-cuisine`  | Recommend by skill + cuisine    |
| GET    | `/api/meta`                         | Cuisine & difficulty enumerations      |
| POST   | `/api/scrape`                       | Scrape BBC Good Food            |

---

## XPath Expressions Used

```xpath
//recipe                                                      # all recipes 
//recipe[@id='r001']                                          # single recipe 
//recipe[difficulty='Intermediate']                           # by skill level 
//recipe[difficulty='Intermediate' and
        (cuisine1='Italian' or cuisine2='Italian')]           # skill + cuisine 
//recipe[cuisine1='Italian' or cuisine2='Italian']            # by cuisine 
//user[1]                                                     # first user 
```

---

## XSD Enumerations

**DifficultyType**: `Beginner | Intermediate | Advanced`

**CuisineType**: `Italian | Asian | Mexican | French | British | Mediterranean | American | Indian | Middle Eastern | Japanese`

