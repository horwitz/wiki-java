/**
 *  @(#)ContributionSurveyor.js 0.01 18/08/2018
 *  Copyright (C) 2018 MER-C
 *
 *  This is free software: you are free to change and redistribute it under the 
 *  Affero GNU GPL version 3 or later, see <https://www.gnu.org/licenses/agpl.html> 
 *  for details. There is NO WARRANTY, to the extent permitted by law.
 */

/**
 *  Toggles between the "user" and "category" modes of the ContributionSurveyor
 *  tool.
 */
document.addEventListener('DOMContentLoaded', function() 
{
    document.getElementById('radio_mode_user').addEventListener('click', function()
    {
        disableElement(document.getElementById('category'));
        document.getElementById('blockedafter').disabled = true;
        enableRequiredElement(document.getElementById('user'));
    });
    
    document.getElementById('radio_mode_category').addEventListener('click', function()
    {
        enableRequiredElement(document.getElementById('category'));
        document.getElementById('blockedafter').disabled = false;
        disableElement(document.getElementById('user'));
    });
});
