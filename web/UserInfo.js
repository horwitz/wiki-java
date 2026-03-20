/**
 *  @(#)UserInfo.js 0.01 17/03/2026
 *  Copyright (C) 2026 MER-C
 *
 *  This is free software: you are free to change and redistribute it under the 
 *  Affero GNU GPL version 3 or later, see <https://www.gnu.org/licenses/agpl.html> 
 *  for details. There is NO WARRANTY, to the extent permitted by law.
 */

/**
 *  Toggles between the "users" and "category" modes of the UserInfo tool.
 */
document.addEventListener('DOMContentLoaded', function() 
{
    document.getElementById('radio_mode_users').addEventListener('click', function()
    {
        disableElement(document.getElementById('category'));
        enableRequiredElement(document.getElementById('users'));
    });
    
    document.getElementById('radio_mode_category').addEventListener('click', function()
    {
        enableRequiredElement(document.getElementById('category'));
        disableElement(document.getElementById('users'));
    });
});
