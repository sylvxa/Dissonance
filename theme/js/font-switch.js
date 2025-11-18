// Web developers please forgive me, I know I have sinned
window.addEventListener('load', function() {
    const toggleFontButton = document.getElementById("toggle-font-button");
    const alternatives = {
        "--font-family": "Virtue, Lucida Grande, Lucida Sans Unicode, Lucida Sans, Geneva, Verdana, sans-serif",
        "--mono-font": "Geneva, 'Courier New', Courier, monospace",
        "--mono-font-size": "27px",
        "--mono-letter-spacing": "2px"
    }

    const altKeys = Object.keys(alternatives);
    const defaults = {}
    const documentElement = document.documentElement;

    altKeys.forEach(key => {
        defaults[key] = documentElement.style.getPropertyValue(key);
    })

    let altFont = localStorage.getItem("alt_font") == "true" ?? false;

    function updateFont() {
        altKeys.forEach(key => {
            documentElement.style.setProperty(key, altFont ? alternatives[key] : defaults[key]);
        })
    }
    
    toggleFontButton.addEventListener("click", () => {
        altFont = !altFont;
        localStorage.setItem("alt_font", altFont);
        updateFont();
    })

    if (altFont) updateFont();
});