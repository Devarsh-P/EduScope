import { useEffect, useState } from 'react'
import { API, fetchJson, postJson } from '../api'

export default function FinalHomePage() {
  const [query, setQuery] = useState('')
  const [loading, setLoading] = useState(false)
  const [showDetails, setShowDetails] = useState(false)

  const [allCourses, setAllCourses] = useState([])
  const [results, setResults] = useState([])
  const [autocomplete, setAutocomplete] = useState([])
  const [spellSuggestions, setSpellSuggestions] = useState([])
  const [recommendation, setRecommendation] = useState(null)
  const [analytics, setAnalytics] = useState(null)
  const [indexPreview, setIndexPreview] = useState({})
  const [frequencyResult, setFrequencyResult] = useState(null)
  const [validation, setValidation] = useState(null)
  const [searchResultsRaw, setSearchResultsRaw] = useState([])
  const [message, setMessage] = useState('')
  const [selectedPlatform, setSelectedPlatform] = useState('')

  useEffect(() => {
    loadInitialData()
  }, [])

  useEffect(() => {
    const timer = setTimeout(async () => {
      const text = query.trim()
      if (!text) {
        setAutocomplete([])
        return
      }

      try {
        const data = await fetchJson(`${API}/autocomplete?prefix=${encodeURIComponent(text.toLowerCase())}`)
        setAutocomplete(data || [])
      } catch {
        setAutocomplete([])
      }
    }, 250)

    return () => clearTimeout(timer)
  }, [query])

  async function loadInitialData() {
    setLoading(true)
    try {
      const courses = await fetchJson(`${API}/courses`)
      setAllCourses(courses || [])
      setResults(courses || [])

      const analyticsData = await fetchJson(`${API}/analytics`)
      setAnalytics(analyticsData || null)

      const indexData = await fetchJson(`${API}/index-preview`)
      setIndexPreview(indexData || {})
    } catch {
      setAllCourses([])
      setResults([])
    } finally {
      setLoading(false)
    }
  }

  async function handleSearch(e) {
    e.preventDefault()

    const text = query.trim()
    if (!text) {
      setResults(allCourses)
      setSpellSuggestions([])
      setRecommendation(null)
      setFrequencyResult(null)
      setValidation(null)
      setSearchResultsRaw([])
      setMessage('Showing all available courses.')
      return
    }

    setLoading(true)

    try {
      const searchData = await postJson(`${API}/search`, { keyword: text })
      const spellData = await fetchJson(`${API}/spellcheck?word=${encodeURIComponent(text.toLowerCase())}`)
      const recommendData = await postJson(`${API}/recommend`, {
        interest: text,
        budget: 'free',
        certificationRequired: false
      })

      let validationData = null
      const looksLikeUrl = text.startsWith('http://') || text.startsWith('https://')
      if (looksLikeUrl) {
        try {
          validationData = await fetchJson(
            `${API}/validate?url=${encodeURIComponent(text)}&priceText=${encodeURIComponent(text)}`
          )
        } catch {
          validationData = null
        }
      }

      const matchedCourses = (searchData || []).map(item => item.course)

      setSearchResultsRaw(searchData || [])
      setResults(matchedCourses)
      setSpellSuggestions(spellData || [])
      setRecommendation(recommendData || null)
      setValidation(validationData)

      if (searchData && searchData.length > 0 && searchData[0]?.course?.id) {
        try {
          const freqData = await fetchJson(
            `${API}/frequency?courseId=${encodeURIComponent(searchData[0].course.id)}&word=${encodeURIComponent(text.toLowerCase())}`
          )
          setFrequencyResult(freqData)
        } catch {
          setFrequencyResult(null)
        }
      } else {
        setFrequencyResult(null)
      }

      try {
        const analyticsData = await fetchJson(`${API}/analytics`)
        setAnalytics(analyticsData || null)
      } catch {
        // ignore
      }

      try {
        const indexData = await fetchJson(`${API}/index-preview`)
        setIndexPreview(indexData || {})
      } catch {
        // ignore
      }

      if (matchedCourses.length > 0) {
        setMessage(`Found ${matchedCourses.length} matching course${matchedCourses.length > 1 ? 's' : ''}.`)
      } else if ((spellData || []).length > 0) {
        setMessage('No exact results found. Try one of the suggested search terms.')
      } else {
        setMessage('No matching courses found.')
      }

      setSelectedPlatform('')
    } catch {
      setResults([])
      setSpellSuggestions([])
      setRecommendation(null)
      setFrequencyResult(null)
      setValidation(null)
      setSearchResultsRaw([])
      setMessage('Something went wrong while searching.')
    } finally {
      setLoading(false)
    }
  }

  function pickSuggestion(word) {
    setQuery(word)
  }

  const platforms = [...new Set(results.map(course => course.platform))]
  const visibleResults = selectedPlatform
    ? results.filter(course => course.platform === selectedPlatform)
    : results

  function platformColor(p) {
    return {
      'Khan Academy': { bg: 'var(--accent-light)', color: 'var(--accent-dark)' },
      'edX': { bg: 'var(--coral-light)', color: 'var(--coral)' },
      'FutureLearn': { bg: 'var(--brand-light)', color: 'var(--brand)' },
      'Class Central': { bg: 'var(--purple-light)', color: 'var(--purple)' },
      'Saylor': { bg: 'var(--warn-light)', color: 'var(--warn)' },
      'Saylor Academy': { bg: 'var(--warn-light)', color: 'var(--warn)' },
    }[p] || { bg: 'var(--surface-deep)', color: 'var(--text-secondary)' }
  }

  return (
    <div className="site-shell">
      <header className="landing-hero">
        <div className="hero-wrap">
          <div className="hero-badge">Search smarter, find better courses</div>
          <h1>EduScope</h1>
          <p className="hero-text">
            Search courses from Khan Academy, edX, FutureLearn, Class Central, and Saylor Academy in one place.
            Get course suggestions, recommendations, and ranked results from a single search.
          </p>

          <form className="search-bar-wrap" onSubmit={handleSearch}>
            <input
              type="text"
              placeholder="Search by course title, skill, or topic..."
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
            <button type="submit" disabled={loading}>
              {loading ? 'Searching...' : 'Search'}
            </button>
          </form>

          {autocomplete.length > 0 && (
            <div className="suggestions-row">
              {autocomplete.map(word => (
                <button
                  key={word}
                  type="button"
                  className="suggestion-chip"
                  onClick={() => pickSuggestion(word)}
                >
                  {word}
                </button>
              ))}
            </div>
          )}
        </div>
      </header>

      <main className="content-wrap">
        {recommendation?.recommendedPlatform && (
          <section className="recommend-box">
            <div>
              <p className="recommend-label">Recommended platform</p>
              <h2>{recommendation.recommendedPlatform}</h2>
              <p>{recommendation.reason}</p>
            </div>
          </section>
        )}

        <div className="top-actions">
          {message && <p className="result-message">{message}</p>}

          <button
            type="button"
            className="details-toggle"
            onClick={() => setShowDetails(!showDetails)}
          >
            {showDetails ? 'Hide Feature Details' : 'Show Feature Details'}
          </button>
        </div>

        {spellSuggestions.length > 0 && (
          <section className="helper-box">
            <p className="helper-title">Suggestions</p>
            <div className="suggestions-row left-align">
              {spellSuggestions.map(word => (
                <button
                  key={word}
                  type="button"
                  className="suggestion-chip"
                  onClick={() => pickSuggestion(word)}
                >
                  {word}
                </button>
              ))}
            </div>
          </section>
        )}

        {platforms.length > 0 && (
          <section className="filter-row">
            <button
              className={`filter-chip ${selectedPlatform === '' ? 'active' : ''}`}
              onClick={() => setSelectedPlatform('')}
            >
              All
            </button>
            {platforms.map(platform => (
              <button
                key={platform}
                className={`filter-chip ${selectedPlatform === platform ? 'active' : ''}`}
                onClick={() => setSelectedPlatform(platform)}
              >
                {platform}
              </button>
            ))}
          </section>
        )}

        <section className="results-grid">
          {visibleResults.map(course => {
            const pc = platformColor(course.platform)
            const isFree = course.pricingModel?.toLowerCase().includes('free')

            return (
              <article className="course-card" key={course.id}>
                <div className="course-top">
                  <span className="platform-pill" style={{ background: pc.bg, color: pc.color }}>
                    {course.platform}
                  </span>
                </div>

                <h3>{course.title}</h3>
                <p>{course.description}</p>

                <div className="course-meta">
                  {course.category && <span className="meta-pill">{course.category}</span>}
                  {course.pricingModel && (
                    <span className={`meta-pill ${isFree ? 'free' : 'paid'}`}>
                      {course.pricingModel}
                    </span>
                  )}
                  {course.certification && !course.certification.toLowerCase().includes('no ') && (
                    <span className="meta-pill cert">Certificate</span>
                  )}
                </div>

                {course.url && (
                  <a href={course.url} target="_blank" rel="noreferrer" className="course-link">
                    View course
                  </a>
                )}
              </article>
            )
          })}

          {!loading && visibleResults.length === 0 && (
            <div className="empty-state">
              <h3>No courses found</h3>
              <p>Try another keyword or topic.</p>
            </div>
          )}
        </section>

        {showDetails && (
          <section className="feature-details">
            <div className="details-grid">
              <div className="detail-card">
                <h3>Autocomplete</h3>
                {autocomplete.length > 0 ? (
                  <div className="chips-wrap">
                    {autocomplete.map(word => (
                      <span key={word} className="mini-chip">{word}</span>
                    ))}
                  </div>
                ) : (
                  <p>No autocomplete output.</p>
                )}
              </div>

              <div className="detail-card">
                <h3>Spell Check</h3>
                {spellSuggestions.length > 0 ? (
                  <div className="chips-wrap">
                    {spellSuggestions.map(word => (
                      <span key={word} className="mini-chip">{word}</span>
                    ))}
                  </div>
                ) : (
                  <p>No spell suggestions.</p>
                )}
              </div>

              <div className="detail-card">
                <h3>Search Ranking</h3>
                {searchResultsRaw.length > 0 ? (
                  <div className="detail-list">
                    {searchResultsRaw.slice(0, 5).map((item, i) => (
                      <div key={`${item.course?.id || i}-${i}`} className="detail-row">
                        <strong>{item.course?.title}</strong>
                        <span>Score: {item.score} | Count: {item.keywordOccurrences}</span>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p>No ranked results yet.</p>
                )}
              </div>

              <div className="detail-card">
                <h3>Frequency Count</h3>
                {frequencyResult ? (
                  <div className="detail-list">
                    <div className="detail-row">
                      <strong>Course</strong>
                      <span>{frequencyResult.title}</span>
                    </div>
                    <div className="detail-row">
                      <strong>Word</strong>
                      <span>{frequencyResult.word}</span>
                    </div>
                    <div className="detail-row">
                      <strong>Count</strong>
                      <span>{frequencyResult.count}</span>
                    </div>
                  </div>
                ) : (
                  <p>No frequency data yet.</p>
                )}
              </div>

              <div className="detail-card">
                <h3>Regex Validation</h3>
                {validation ? (
                  <div className="detail-list">
                    <div className="detail-row">
                      <strong>Valid URL</strong>
                      <span>{String(validation.validUrl)}</span>
                    </div>
                    <div className="detail-row">
                      <strong>Valid Price</strong>
                      <span>{String(validation.validPrice)}</span>
                    </div>
                    <div className="detail-row">
                      <strong>Prices Found</strong>
                      <span>
                        {(validation.pricesFound || []).length > 0
                          ? validation.pricesFound.join(', ')
                          : 'None'}
                      </span>
                    </div>
                  </div>
                ) : (
                  <p>Regex validation appears when the search input looks like a URL.</p>
                )}
              </div>

              <div className="detail-card">
                <h3>Analytics</h3>
                {analytics ? (
                  <div className="detail-list">
                    <div className="detail-row">
                      <strong>Total Courses</strong>
                      <span>{analytics.totalCourses}</span>
                    </div>
                    <div className="detail-row">
                      <strong>Vocabulary Sample</strong>
                      <span>{analytics.vocabularySample?.length || 0}</span>
                    </div>
                    <div className="detail-row multi">
                      <strong>Platform Counts</strong>
                      <span>
                        {Object.entries(analytics.platformCounts || {})
                          .map(([k, v]) => `${k}: ${v}`)
                          .join(' | ')}
                      </span>
                    </div>
                    <div className="detail-row multi">
                      <strong>Search Frequency</strong>
                      <span>
                        {Object.entries(analytics.searchFrequency || {}).length > 0
                          ? Object.entries(analytics.searchFrequency || {})
                              .map(([k, v]) => `${k}: ${v}`)
                              .join(' | ')
                          : 'No searches yet'}
                      </span>
                    </div>
                  </div>
                ) : (
                  <p>No analytics available.</p>
                )}
              </div>

              <div className="detail-card full-width">
                <h3>Inverted Index Preview</h3>
                {Object.keys(indexPreview).length > 0 ? (
                  <div className="detail-list">
                    {Object.entries(indexPreview).map(([word, ids]) => (
                      <div key={word} className="detail-row multi">
                        <strong>{word}</strong>
                        <span>{Array.isArray(ids) ? ids.join(', ') : ''}</span>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p>No index preview available.</p>
                )}
              </div>
            </div>
          </section>
        )}
      </main>
    </div>
  )
}