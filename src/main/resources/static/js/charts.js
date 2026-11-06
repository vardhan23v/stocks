/*
 * StockSense charts and small page helpers (vanilla JavaScript + Chart.js 4).
 *
 * Chart data never comes from this file: the backing beans serialise stored
 * database rows to JSON and put them in a data-chart attribute, for example
 *   <div class="ss-chart" data-chart-kind="price" data-chart="{...}">
 * Every chart renders an empty state instead of inventing values when its
 * series is missing.
 */
(function () {
    "use strict";

    var css = getComputedStyle(document.documentElement);
    function token(name, fallback) {
        var value = css.getPropertyValue(name).trim();
        return value || fallback;
    }

    var C = {
        text: token("--ss-text", "#e7e9ee"),
        muted: token("--ss-text-muted", "#9ba4b4"),
        faint: token("--ss-text-faint", "#6f788a"),
        grid: token("--ss-border", "#2a303c"),
        accent: token("--ss-accent", "#7b93ff"),
        up: token("--ss-up", "#2fcf73"),
        down: token("--ss-down", "#ff5c5c"),
        warn: token("--ss-warn", "#f5b13d"),
        series: ["#7b93ff", "#3fc9a0", "#f5b13d", "#e879f9", "#38bdf8"]
    };

    function baseOptions(extra) {
        var options = {
            responsive: true,
            maintainAspectRatio: false,
            animation: window.matchMedia("(prefers-reduced-motion: reduce)").matches ? false : { duration: 250 },
            interaction: { mode: "index", intersect: false },
            plugins: {
                legend: { labels: { color: C.muted, boxWidth: 12, boxHeight: 2, font: { size: 11 } } },
                tooltip: {
                    backgroundColor: "#0b0d12",
                    borderColor: C.grid,
                    borderWidth: 1,
                    titleColor: C.text,
                    bodyColor: C.muted
                }
            },
            scales: {
                x: {
                    ticks: { color: C.faint, maxRotation: 0, autoSkip: true, maxTicksLimit: 7, font: { size: 11 } },
                    grid: { display: false },
                    border: { color: C.grid }
                },
                y: {
                    position: "right",
                    ticks: { color: C.faint, font: { size: 11 } },
                    grid: { color: C.grid },
                    border: { display: false }
                }
            }
        };
        return Object.assign(options, extra || {});
    }

    function line(label, data, color, extra) {
        return Object.assign({
            type: "line",
            label: label,
            data: data,
            borderColor: color,
            backgroundColor: color,
            borderWidth: 1.6,
            pointRadius: 0,
            pointHoverRadius: 3,
            tension: 0.15,
            spanGaps: true
        }, extra || {});
    }

    function hasValues(array) {
        return Array.isArray(array) && array.some(function (v) { return v !== null && v !== undefined; });
    }

    function empty(element, message) {
        element.innerHTML = "";
        var box = document.createElement("div");
        box.className = "ss-empty";
        box.textContent = message;
        element.appendChild(box);
    }

    function canvasIn(element, label) {
        var canvas = document.createElement("canvas");
        canvas.setAttribute("role", "img");
        canvas.setAttribute("aria-label", label);
        element.innerHTML = "";
        element.appendChild(canvas);
        return canvas;
    }

    var builders = {
        price: function (el, d) {
            if (!hasValues(d.close)) { return empty(el, "No stored price history for this range."); }
            var first = d.close.find(function (v) { return v !== null; });
            var last = d.close[d.close.length - 1];
            var color = last >= first ? C.up : C.down;
            new Chart(canvasIn(el, "Closing price chart for " + d.symbol), {
                data: {
                    labels: d.labels,
                    datasets: [
                        line("Close", d.close, color, {
                            borderWidth: 2,
                            fill: { target: "origin", above: color + "1a", below: color + "1a" }
                        }),
                        line("SMA 20", d.sma20, C.accent, { borderDash: [4, 3] }),
                        line("SMA 50", d.sma50, C.warn, { borderDash: [6, 4] }),
                        line("EMA 20", d.ema20, "#e879f9", { borderWidth: 1.2, hidden: true })
                    ]
                },
                options: baseOptions({
                    scales: Object.assign(baseOptions().scales, {
                        y: Object.assign(baseOptions().scales.y, { grace: "4%" })
                    })
                })
            });
        },

        volume: function (el, d) {
            if (!hasValues(d.volume)) { return empty(el, "The data source did not provide volume."); }
            var colors = d.close.map(function (value, i) {
                return i > 0 && value !== null && d.close[i - 1] !== null && value < d.close[i - 1] ? C.down + "aa" : C.up + "aa";
            });
            new Chart(canvasIn(el, "Trading volume chart"), {
                type: "bar",
                data: { labels: d.labels, datasets: [{ label: "Volume", data: d.volume, backgroundColor: colors, borderWidth: 0 }] },
                options: baseOptions({
                    plugins: { legend: { display: false } },
                    scales: Object.assign(baseOptions().scales, {
                        y: Object.assign(baseOptions().scales.y, {
                            ticks: {
                                color: C.faint, font: { size: 11 },
                                callback: function (v) {
                                    return v >= 1e9 ? (v / 1e9).toFixed(1) + "B" : v >= 1e6 ? (v / 1e6).toFixed(1) + "M"
                                         : v >= 1e3 ? (v / 1e3).toFixed(0) + "K" : v;
                                }
                            }
                        })
                    })
                })
            });
        },

        rsi: function (el, d) {
            if (!hasValues(d.rsi)) { return empty(el, "Not enough history to calculate RSI(14)."); }
            var band = function (value) { return d.labels.map(function () { return value; }); };
            new Chart(canvasIn(el, "RSI 14 chart"), {
                data: {
                    labels: d.labels,
                    datasets: [
                        line("RSI 14", d.rsi, C.accent, { borderWidth: 1.8 }),
                        line("70 overbought", band(70), C.down + "99", { borderDash: [4, 4], borderWidth: 1, pointHoverRadius: 0 }),
                        line("30 oversold", band(30), C.up + "99", { borderDash: [4, 4], borderWidth: 1, pointHoverRadius: 0 })
                    ]
                },
                options: baseOptions({
                    scales: Object.assign(baseOptions().scales, {
                        y: Object.assign(baseOptions().scales.y, { min: 0, max: 100, ticks: { color: C.faint, stepSize: 30, font: { size: 11 } } })
                    })
                })
            });
        },

        macd: function (el, d) {
            if (!hasValues(d.macd)) { return empty(el, "Not enough history to calculate MACD(12, 26, 9)."); }
            new Chart(canvasIn(el, "MACD chart"), {
                data: {
                    labels: d.labels,
                    datasets: [
                        line("MACD", d.macd, C.accent, { borderWidth: 1.8 }),
                        line("Signal", d.signal, C.warn, { borderWidth: 1.4 }),
                        {
                            type: "bar",
                            label: "Histogram",
                            data: d.histogram,
                            backgroundColor: d.histogram.map(function (v) { return v !== null && v < 0 ? C.down + "88" : C.up + "88"; }),
                            borderWidth: 0
                        }
                    ]
                },
                options: baseOptions()
            });
        },

        performance: function (el, d) {
            if (!d.datasets || !d.datasets.length) { return empty(el, "No stored price history to chart yet."); }
            new Chart(canvasIn(el, "Performance chart, percent change"), {
                data: {
                    labels: d.labels,
                    datasets: d.datasets.map(function (set, i) {
                        return line(set.label, set.data, C.series[i % C.series.length], { borderWidth: 1.8 });
                    })
                },
                options: baseOptions({
                    scales: Object.assign(baseOptions().scales, {
                        y: Object.assign(baseOptions().scales.y, {
                            ticks: { color: C.faint, font: { size: 11 }, callback: function (v) { return v + "%"; } }
                        })
                    }),
                    plugins: Object.assign(baseOptions().plugins, {
                        tooltip: Object.assign(baseOptions().plugins.tooltip, {
                            callbacks: {
                                label: function (ctx) {
                                    return ctx.parsed.y === null ? null : ctx.dataset.label + ": " + (ctx.parsed.y > 0 ? "+" : "") + ctx.parsed.y.toFixed(2) + "%";
                                }
                            }
                        })
                    })
                })
            });
        }
    };

    function renderCharts() {
        document.querySelectorAll("[data-chart]").forEach(function (el) {
            var kind = el.getAttribute("data-chart-kind");
            var data;
            try {
                data = JSON.parse(el.getAttribute("data-chart") || "{}");
            } catch (e) {
                return empty(el, "Chart data could not be read.");
            }
            if (typeof Chart === "undefined") {
                return empty(el, "The chart library could not be loaded (offline?). The data tables below still show every value.");
            }
            if (builders[kind]) {
                builders[kind](el, data);
            }
        });
    }

    /* Plain GET forms: restore the selected option from data-value="...". */
    function restoreSelects() {
        document.querySelectorAll("select[data-value]").forEach(function (select) {
            select.value = select.getAttribute("data-value");
        });
    }

    /* Auto-submit a GET filter form when one of its selects changes. */
    function autoSubmit() {
        document.querySelectorAll("form[data-autosubmit] select").forEach(function (select) {
            select.addEventListener("change", function () { select.form.submit(); });
        });
    }

    document.addEventListener("DOMContentLoaded", function () {
        restoreSelects();
        autoSubmit();
        renderCharts();
    });
})();
