<%--
  Stock details: /stocks/{id}?range=3M. StockDetailsService prepares "d"
  (a StockDetailsView); charts get their data as JSON in data-chart attributes.
--%>
<%@ page pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/common/taglibs.jspf" %>
<ss:page title="${empty d ? 'Stock details' : d.stock.symbol}">
    <ss:messages/>

    <c:if test="${empty d}">
        <div class="ss-card ss-empty">
            <div class="ss-empty-icon"><i class="bi bi-question-lg" aria-hidden="true"></i></div>
            <h1 class="h4">Stock not found</h1>
            <p>That stock does not exist or is no longer listed. The link may be incomplete.</p>
            <a class="btn btn-ghost" href="${ctx}/stocks">Back to the stock explorer</a>
        </div>
    </c:if>

    <c:if test="${not empty d}">
        <c:set var="s" value="${d.stock}"/>
        <c:set var="ind" value="${d.indicator}"/>
        <c:set var="rec" value="${d.recommendation}"/>
        <c:set var="na" value="Not available"/>
        <c:set var="insufficient" value="Insufficient data"/>

        <nav aria-label="Breadcrumb" class="small mb-2">
            <a href="${ctx}/stocks">Stocks</a> <span class="ss-faint">/</span> <span class="ss-muted"><c:out value="${s.symbol}"/></span>
        </nav>

        <%-- Header: name, price, provenance, watchlist button --%>
        <div class="ss-card mb-3">
            <div class="row g-3 align-items-center">
                <div class="col-lg-7">
                    <div class="d-flex flex-wrap align-items-center gap-2 mb-1">
                        <h1 class="m-0"><c:out value="${s.companyName}"/></h1>
                        <c:if test="${not s.active}"><span class="ss-rec SELL">Inactive</span></c:if>
                    </div>
                    <div class="d-flex flex-wrap gap-2 align-items-center ss-muted small">
                        <span class="ss-symbol fs-6"><c:out value="${s.symbol}"/></span>
                        <span class="ss-badge-source"><c:out value="${s.exchange}"/></span>
                        <span><c:out value="${s.sector}"/></span><span class="ss-faint">&#183;</span><span><c:out value="${s.industry}"/></span>
                    </div>
                </div>
                <div class="col-lg-5 text-lg-end">
                    <c:choose>
                        <c:when test="${s.hasPrice}">
                            <div class="ss-price-big">${s.currencySymbol}${fmt.money(s.latestPrice)}</div>
                            <div class="mt-1">
                                <ss:priceChange stock="${s}" compact="false"/>
                                <span class="small ss-faint ms-1">vs previous close</span>
                            </div>
                            <div class="small ss-muted mt-2">
                                Close of ${fmt.day(d.newestDate)} &#183; <ss:dataBadge source="${s.dataSource}"/>
                            </div>
                        </c:when>
                        <c:otherwise><div class="ss-muted">No price data is stored for this stock yet.</div></c:otherwise>
                    </c:choose>
                </div>
            </div>
            <hr class="my-3"/>
            <div class="d-flex flex-wrap justify-content-between align-items-center gap-2">
                <p class="small ss-muted m-0">
                    Source: <strong><c:out value="${d.dataSourceLabel}"/></strong> &#183; ${d.barCount} stored daily bars.
                    Prices are end-of-day closes, not live quotes.
                </p>
                <form action="${ctx}${d.watched ? '/watchlist/remove' : '/watchlist/add'}" method="post" class="m-0">
                    <sec:csrfInput/>
                    <input type="hidden" name="stockId" value="${s.id}"/>
                    <input type="hidden" name="returnTo" value="${fn:escapeXml(currentUrl)}"/>
                    <button type="submit" class="btn ${d.watched ? 'btn-ghost' : 'btn-primary'}">${d.watched ? 'Remove from watchlist' : '&#9733; Add to watchlist'}</button>
                </form>
            </div>
        </div>

        <%-- Key statistics --%>
        <div class="row g-3 mb-3">
            <div class="col-6 col-md-4 col-xl-2"><div class="ss-stat">
                <div class="ss-stat-label">Volume</div>
                <div class="ss-stat-value ss-stat-small num">${not empty s.latestVolume ? fmt.whole(s.latestVolume) : na}</div>
                <div class="ss-stat-note">Latest session</div>
            </div></div>
            <div class="col-6 col-md-4 col-xl-2"><div class="ss-stat">
                <div class="ss-stat-label">${d.range} change</div>
                <div class="ss-stat-value ss-stat-small num ${d.rangeDirection}">${fmt.percent(d.rangeChangePercent)}</div>
                <div class="ss-stat-note">Selected range</div>
            </div></div>
            <div class="col-6 col-md-4 col-xl-2"><div class="ss-stat">
                <div class="ss-stat-label">52-week high</div>
                <div class="ss-stat-value ss-stat-small num">${not empty d.high52 ? fmt.money(d.high52) : na}</div>
                <div class="ss-stat-note"><c:out value="${d.range52Note}"/></div>
            </div></div>
            <div class="col-6 col-md-4 col-xl-2"><div class="ss-stat">
                <div class="ss-stat-label">52-week low</div>
                <div class="ss-stat-value ss-stat-small num">${not empty d.low52 ? fmt.money(d.low52) : na}</div>
                <div class="ss-stat-note"><c:out value="${d.range52Note}"/></div>
            </div></div>
            <div class="col-6 col-md-4 col-xl-2"><div class="ss-stat">
                <div class="ss-stat-label">Market cap</div>
                <div class="ss-stat-value ss-stat-small num">${not empty s.marketCap ? fmt.whole(s.marketCap) : na}</div>
                <div class="ss-stat-note"><c:out value="${empty s.marketCap ? 'Only shown when a provider reports it' : s.currency += ' · ' += s.fundamentalsSource}"/></div>
            </div></div>
            <div class="col-6 col-md-4 col-xl-2"><div class="ss-stat">
                <div class="ss-stat-label">Volatility</div>
                <div class="ss-stat-value ss-stat-small num">${not empty ind.volatility ? fmt.decimal(ind.volatility, '0.0') += '%' : insufficient}</div>
                <div class="ss-stat-note">Annualised, last 60 sessions</div>
            </div></div>
        </div>

        <div class="row g-3 mb-3">
            <%-- Charts --%>
            <div class="col-xl-8">
                <div class="ss-card">
                    <div class="ss-card-head flex-wrap">
                        <h2>Price history</h2>
                        <div class="ss-segmented" role="group" aria-label="Chart range">
                            <span class="is-disabled" title="Intraday data is not available: the provider supplies end-of-day bars only">1D</span>
                            <c:forEach var="r" items="${d.ranges}">
                                <c:choose>
                                    <c:when test="${r.available}">
                                        <a href="${ctx}/stocks/${s.id}?range=${r.code}" title="${r.label}" class="${r.code == d.range ? 'is-active' : ''}">${r.code}</a>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="is-disabled" title="Not enough stored history for ${r.label}">${r.code}</span>
                                    </c:otherwise>
                                </c:choose>
                            </c:forEach>
                        </div>
                    </div>
                    <c:if test="${d.sampleData}">
                        <p class="small mb-2"><span class="ss-badge-sample">Sample data</span> <span class="ss-muted">Synthetic series for demonstration - not real prices.</span></p>
                    </c:if>
                    <c:set var="chart" value="${fn:escapeXml(d.chartJson)}"/>
                    <div class="ss-chart ss-chart-lg" data-chart-kind="price" data-chart="${chart}"></div>
                    <h3 class="mt-4 mb-2 small ss-faint text-uppercase">Volume</h3>
                    <div class="ss-chart ss-chart-sm" data-chart-kind="volume" data-chart="${chart}"></div>
                    <div class="row g-3 mt-1">
                        <div class="col-md-6">
                            <h3 class="mb-2 small ss-faint text-uppercase">RSI (14)</h3>
                            <div class="ss-chart ss-chart-md" data-chart-kind="rsi" data-chart="${chart}"></div>
                        </div>
                        <div class="col-md-6">
                            <h3 class="mb-2 small ss-faint text-uppercase">MACD (12, 26, 9)</h3>
                            <div class="ss-chart ss-chart-md" data-chart-kind="macd" data-chart="${chart}"></div>
                        </div>
                    </div>
                </div>
            </div>

            <%-- Recommendation --%>
            <div class="col-xl-4">
                <div class="ss-rec-card ${not empty rec ? rec.recommendationType : ''}">
                    <div class="d-flex justify-content-between align-items-center">
                        <h2 class="m-0 fs-6">Rule-based recommendation</h2>
                        <ss:recBadge rec="${rec}"/>
                    </div>
                    <c:if test="${not empty rec}">
                        <c:if test="${not rec.insufficient}">
                            <div>
                                <div class="d-flex justify-content-between small ss-faint mb-1"><span>SELL</span><span>Score <span class="num ss-muted">${rec.recommendationScore}</span></span><span>BUY</span></div>
                                <ss:scoreBar rec="${rec}"/>
                            </div>
                        </c:if>
                        <ul class="ss-reasons">
                            <c:forEach var="reason" items="${rec.reasons}"><li><c:out value="${reason}"/></li></c:forEach>
                        </ul>
                        <div class="small ss-faint">
                            Generated ${fmt.dateTime(rec.generatedAt)}
                            from data of ${fmt.date(rec.dataTimestamp)}
                            (<c:out value="${rec.dataSource == 'SAMPLE' ? 'SAMPLE DATA' : rec.dataSource}"/>).
                        </div>
                    </c:if>
                    <p class="ss-disclaimer m-0">
                        Educational output of fixed technical rules, not personalised financial advice.
                        <a href="${ctx}/recommendations#methodology">How the score works</a>
                    </p>
                </div>
            </div>
        </div>

        <%-- Indicators --%>
        <div class="ss-card mb-3">
            <div class="ss-card-head">
                <h2>Technical indicators</h2>
                <span class="small ss-faint">Latest values, calculated in Java from ${d.barCount} stored bars</span>
            </div>
            <div class="row g-3">
                <div class="col-sm-6 col-lg-3"><div class="ss-indicator">
                    <div class="ss-indicator-name">SMA 20</div>
                    <div class="ss-indicator-value num">${not empty ind.sma ? fmt.money(ind.sma) : insufficient}</div>
                    <div class="ss-indicator-help">
                        Average close of the last 20 sessions.
                        <c:if test="${not empty d.distanceFromSma}">The price is
                            <span class="num ${d.distanceFromSma.signum() >= 0 ? 'up' : 'down'}">${d.distanceFromSma}%</span> from it.</c:if>
                    </div>
                </div></div>
                <div class="col-sm-6 col-lg-3"><div class="ss-indicator">
                    <div class="ss-indicator-name">SMA 50</div>
                    <div class="ss-indicator-value num">${not empty ind.smaLong ? fmt.money(ind.smaLong) : insufficient}</div>
                    <div class="ss-indicator-help">Longer-term trend. SMA 20 above SMA 50 is commonly read as a short-term uptrend.</div>
                </div></div>
                <div class="col-sm-6 col-lg-3"><div class="ss-indicator">
                    <div class="ss-indicator-name">EMA 20</div>
                    <div class="ss-indicator-value num">${not empty ind.ema ? fmt.money(ind.ema) : insufficient}</div>
                    <div class="ss-indicator-help">Like SMA 20 but weights recent closes more, so it reacts faster.</div>
                </div></div>
                <div class="col-sm-6 col-lg-3"><div class="ss-indicator">
                    <div class="ss-indicator-name">RSI 14</div>
                    <div class="ss-indicator-value num">${not empty ind.rsi ? fmt.decimal(ind.rsi, '0.0') : insufficient}</div>
                    <div class="ss-indicator-help"><c:out value="${ind.rsiZone}"/>.</div>
                </div></div>
                <div class="col-sm-6 col-lg-3"><div class="ss-indicator">
                    <div class="ss-indicator-name">MACD</div>
                    <div class="ss-indicator-value num">${not empty ind.macd ? fmt.decimal(ind.macd, '0.000') : insufficient}</div>
                    <div class="ss-indicator-help">EMA 12 minus EMA 26: positive when short-term momentum is stronger.</div>
                </div></div>
                <div class="col-sm-6 col-lg-3"><div class="ss-indicator">
                    <div class="ss-indicator-name">Signal line</div>
                    <div class="ss-indicator-value num">${not empty ind.signalLine ? fmt.decimal(ind.signalLine, '0.000') : insufficient}</div>
                    <div class="ss-indicator-help">
                        9-period EMA of MACD.
                        <c:if test="${not empty ind.macdHistogram}">MACD is ${ind.macdHistogram.signum() >= 0 ? 'above' : 'below'} it
                            (histogram <span class="num">${ind.macdHistogram}</span>).</c:if>
                    </div>
                </div></div>
                <div class="col-sm-6 col-lg-3"><div class="ss-indicator">
                    <div class="ss-indicator-name">20-session change</div>
                    <div class="ss-indicator-value num ${empty ind.momentum ? '' : (ind.momentum.signum() >= 0 ? 'up' : 'down')}">${not empty ind.momentum ? fmt.percent(ind.momentum) : insufficient}</div>
                    <div class="ss-indicator-help">Price momentum over roughly one month of trading.</div>
                </div></div>
                <div class="col-sm-6 col-lg-3"><div class="ss-indicator">
                    <div class="ss-indicator-name">Volume vs average</div>
                    <div class="ss-indicator-value num">${not empty ind.volumeRatio ? fmt.decimal(ind.volumeRatio, '0.00') += '&#215;' : na}</div>
                    <div class="ss-indicator-help">Latest volume divided by the 20-session average.</div>
                </div></div>
            </div>
            <p class="small ss-faint mt-3 mb-0">These readings are common analytical interpretations, not guaranteed buy or sell signals.</p>
        </div>

        <div class="row g-3">
            <div class="col-lg-8">
                <div class="ss-card ss-card-flush">
                    <div class="ss-card-head">
                        <h2>Daily prices &#183; ${d.range}</h2>
                        <ss:dataBadge source="${d.dataSource}"/>
                    </div>
                    <c:choose>
                        <c:when test="${empty d.rangeBars}"><div class="ss-empty">No stored prices for this range.</div></c:when>
                        <c:otherwise>
                            <div class="table-responsive ss-scroll mt-2">
                                <table class="table table-sm ss-table">
                                    <thead>
                                        <tr>
                                            <th scope="col">Date</th>
                                            <th scope="col" class="num text-end">Open</th>
                                            <th scope="col" class="num text-end">High</th>
                                            <th scope="col" class="num text-end">Low</th>
                                            <th scope="col" class="num text-end">Close</th>
                                            <th scope="col" class="num text-end">Volume</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <c:forEach var="b" items="${d.rangeBars}">
                                            <tr>
                                                <td class="num">${fmt.date(b.recordedAt)}</td>
                                                <td class="num text-end">${fmt.money(b.openPrice)}</td>
                                                <td class="num text-end">${fmt.money(b.highPrice)}</td>
                                                <td class="num text-end">${fmt.money(b.lowPrice)}</td>
                                                <td class="num text-end fw-semibold">${fmt.money(b.closePrice)}</td>
                                                <td class="num text-end ss-muted">${fmt.whole(b.volume)}</td>
                                            </tr>
                                        </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
            <div class="col-lg-4">
                <div class="ss-card">
                    <h2 class="fs-6">About <c:out value="${s.companyName}"/></h2>
                    <p class="ss-muted small"><c:out value="${empty s.description ? 'No description has been added.' : s.description}"/></p>
                    <dl class="row small mb-0">
                        <dt class="col-5 ss-faint fw-normal">Ticker</dt><dd class="col-7"><c:out value="${s.symbol}"/></dd>
                        <dt class="col-5 ss-faint fw-normal">Exchange</dt><dd class="col-7"><c:out value="${s.exchange}"/></dd>
                        <dt class="col-5 ss-faint fw-normal">Provider symbol</dt><dd class="col-7 mono"><c:out value="${s.apiSymbol}"/></dd>
                        <dt class="col-5 ss-faint fw-normal">Currency</dt><dd class="col-7"><c:out value="${s.currency}"/></dd>
                        <dt class="col-5 ss-faint fw-normal">Sector</dt><dd class="col-7"><c:out value="${s.sector}"/></dd>
                        <dt class="col-5 ss-faint fw-normal">Industry</dt><dd class="col-7 mb-0"><c:out value="${s.industry}"/></dd>
                    </dl>
                </div>
            </div>
        </div>
    </c:if>
</ss:page>
