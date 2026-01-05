/**
 *  @(#)StringComparison.js 0.01 05/01/2026
 *  Copyright (C) 2026 MER-C
 *
 *  This is free software: you are free to change and redistribute it under the 
 *  Affero GNU GPL version 3 or later, see <https://www.gnu.org/licenses/agpl.html> 
 *  for details. There is NO WARRANTY, to the extent permitted by law.
 */

/**
 *  Toggles between the modes of the String Comparison tool.
 */
document.addEventListener('DOMContentLoaded', function() 
{
    document.getElementById('radio_wikipage1').addEventListener('click', function()
    {
        disableElement(document.getElementById('text1'));
        enableRequiredElement(document.getElementById('wiki1'));
        enableRequiredElement(document.getElementById('page1'));
    });
    
    document.getElementById('radio_text1').addEventListener('click', function()
    {
        enableRequiredElement(document.getElementById('text1'));
        disableElement(document.getElementById('wiki1'));
        disableElement(document.getElementById('page1'));
    });
    
    document.getElementById('radio_wikipage2').addEventListener('click', function()
    {
        disableElement(document.getElementById('text2'));
        enableRequiredElement(document.getElementById('wiki2'));
        enableRequiredElement(document.getElementById('page2'));
    });
    
    document.getElementById('radio_text2').addEventListener('click', function()
    {
        enableRequiredElement(document.getElementById('text2'));
        disableElement(document.getElementById('wiki2'));
        disableElement(document.getElementById('page2'));
    });
});
