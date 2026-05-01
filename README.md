# Online Education Platform Analysis - Website Version

This is a **full website version** of the project:
- **Backend:** Java Spring Boot
- **Frontend:** React + Vite

## What it includes
- Web crawling architecture with JSoup
- HTML parsing
- Inverted indexing
- Page ranking
- Frequency count
- Search frequency tracking
- Spell checking using edit distance
- Word completion using Trie
- Regex validation and pattern finding
- Recommendation system

## Important reality check
Real sites like Coursera and Udemy are dynamic. So this project uses:
1. **Live fetch attempt** of homepage HTML using JSoup
2. **Fallback sample course data** for stable demo output

That means your demo will work even if live crawling changes.

## Backend setup
```bash
cd backend
mvn spring-boot:run
```
Backend URL:
- http://localhost:8081/api/health

## Frontend setup
Open a new terminal:
```bash
cd frontend
npm install
npm run dev
```
Frontend URL:
- http://localhost:5173

## Suggested presentation flow
1. Refresh crawl data
2. Show crawled course cards
3. Search a keyword and show ranking
4. Show autocomplete words
5. Show spell suggestions for a wrong word
6. Show analytics / search frequency
7. Show regex validation for URL and price text
8. Show recommendation system

## Files you should mention in report
### Backend
- `PlatformCrawlerService.java`
- `AnalysisService.java`
- `ApiController.java`
- `Trie.java`
- `TextUtils.java`

### Frontend
- `App.jsx`
- `styles.css`

## What you should improve before submission
- Replace fallback/sample data with more exact selectors per platform
- Add CSV export if your team wants extra feature points
- Take screenshots of every required feature
- Write report sections mapping each student to Java files and algorithms


## Updated crawl behavior
- Default crawl now targets 10 configured topics: python, java, machine learning, data science, ai, aws, c++, web development, salesforce, business analyst.
- The crawler keeps up to 30 results per platform and saves them to `backend/data/courses.csv`.
- The CSV file is overwritten on every fresh crawl, so old rows are removed automatically.
