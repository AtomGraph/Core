/* 
 * Copyright (C) 2013 Martynas Jusevičius <martynas@graphity.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


function generateUUID()
{
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
	var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
	return v.toString(16);
    });
}

function cloneUniqueObject(controlGroupElement, newId)
{
    controlGroupElement.id = "control-group-" + newId;

    var controlsElement = controlGroupElement.children[controlGroupElement.children.length - 1];
    controlsElement.id = "controls-" + newId;

    // "Remove object" button
    controlsElement.children[0].onclick = function() { removeObject(newId); };

    // tab headings list
    var tabList = controlsElement.children[1];
    tabList.children[0].id = "li-ou-" + newId;
    tabList.children[0].onclick = function() { toggleObjectTabs("ou", newId); };
    tabList.children[1].id = "li-ob-" + newId;
    tabList.children[1].onclick = function() { toggleObjectTabs("ob", newId); };
    tabList.children[2].id = "li-olll-" + newId;
    tabList.children[2].onclick = function() { toggleObjectTabs("olll", newId); };
    tabList.children[3].id = "li-ollt-" + newId;
    tabList.children[3].onclick = function() { toggleObjectTabs("ollt", newId); };

    // tab panes
    var ouDiv = controlsElement.children[2];
    ouDiv.id = "div-ou-" + newId;
    ouDiv.children[0].removeAttribute("value");
    var obDiv = controlsElement.children[3];
    obDiv.id = "div-ob-" + newId;
    obDiv.children[0].removeAttribute("value");
    var olllDiv = controlsElement.children[4];
    olllDiv.id = "div-olll-" + newId;
    olllDiv.children[0].removeAttribute("value");
    var olltDiv = controlsElement.children[5];
    olltDiv.id = "div-ollt-" + newId;
    olltDiv.children[0].removeAttribute("value");
    //alert(ouDiv);

    return controlGroupElement;
}

function toggleObjectTabs(type, id)
{
    var types = new Array("ou", "ob", "olll", "ollt");

    for (var i = 0; i < types.length; i++)
    {
	var tabListItem = document.getElementById("li-" + types[i] + "-" + id);
	if (type === types[i]) tabListItem.className = 'active';
	else tabListItem.className = '';

	var tabPaneDiv = document.getElementById("div-" + types[i] + "-" + id);
	if (type === types[i]) tabPaneDiv.style.display = 'block';
	else tabPaneDiv.style.display = 'none';
    }		    
}

function removeObject(id)
{
    document.getElementById("control-group-" + id).style.display = 'none';
}