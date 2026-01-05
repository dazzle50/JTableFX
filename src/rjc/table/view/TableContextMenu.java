/**************************************************************************
 *  Copyright (C) 2026 by Richard Crook                                   *
 *  https://github.com/dazzle50/JTableFX                                  *
 *                                                                        *
 *  This program is free software: you can redistribute it and/or modify  *
 *  it under the terms of the GNU General Public License as published by  *
 *  the Free Software Foundation, either version 3 of the License, or     *
 *  (at your option) any later version.                                   *
 *                                                                        *
 *  This program is distributed in the hope that it will be useful,       *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *  GNU General Public License for more details.                          *
 *                                                                        *
 *  You should have received a copy of the GNU General Public License     *
 *  along with this program.  If not, see http://www.gnu.org/licenses/    *
 **************************************************************************/

package rjc.table.view;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javafx.scene.control.ContextMenu;

/*************************************************************************************************/
/****************************** Context menu manager for table-view ******************************/
/*************************************************************************************************/

/**
 * Manages context menu configuration and display for table cells.
 * Provides static methods to show context menus, configure which menu items
 * should be enabled (defaults to all enabled if not configured for specific view),
 * and determine the appropriate menu based on cell location.
 * Delegates the actual menu construction to {@link TableContextMenuBuilder}.
 */
public class TableContextMenu
{
  // optional context menu items that can be enabled/disabled for context menu
  public enum TableMenuItems
  {
    COLUMN_HIDE, ROW_HIDE, COLUMN_SHOW, ROW_SHOW, COLUMN_FILTER_TEXT, ROW_FILTER_TEXT, COLUMN_SORT, ROW_SORT, COLUMN_INSERT, ROW_INSERT, COLUMN_DELETE, ROW_DELETE
  }

  private static final Map<TableView, Set<TableMenuItems>> ENABLED = new WeakHashMap<>();

  /****************************************** constructor *****************************************/
  private TableContextMenu()
  {
    // private constructor to prevent instantiation of utility class
  }

  /********************************************* show ********************************************/
  /**
   * Shows the context menu for the specified table view at the given screen coordinates.
   * Determines which menu items to display based on the current mouse cell position.
   *
   * @param view the table view to show the context menu for
   * @param x the x screen coordinate where the menu should appear
   * @param y the y screen coordinate where the menu should appear
   */
  public static ContextMenu show( TableView view, double x, double y )
  {
    // if no table-view provided, cannot show menu
    if ( view == null )
      return null;

    // get the cell coordinates where the context menu was triggered
    int mouseRow = view.getMouseCell().getRow();
    int mouseCol = view.getMouseCell().getColumn();

    // use builder to construct the appropriate menu for the location
    var menu = new TableContextMenuBuilder( view, mouseRow, mouseCol ).build();

    // show the context menu if it has any items
    if ( menu != null && !menu.getItems().isEmpty() )
      menu.show( view.getScene().getWindow(), x, y );

    return menu;
  }

  /******************************************** enable *******************************************/
  /**
   * Adds specified optional menu items to the context menu for the specified table view.
   * 
   * @param view the table view to configure
   * @param items the menu items to enable for the context menu
   */
  public static void enable( TableView view, TableMenuItems... items )
  {
    // enable specified context menu items for this table-view
    var set = ENABLED.get( view );
    if ( set == null )
    {
      set = EnumSet.noneOf( TableMenuItems.class );
      ENABLED.put( view, set );
    }
    for ( var item : items )
      set.add( item );
  }

  /****************************************** enableAll ******************************************/
  /**
   * Enables all optional menu items for the context menu for the specified table view.
   * Relies on default behaviour of all enabled if no specific configuration exists.
   *
   * @param view the table view to configure
   */
  public static void enableAll( TableView view )
  {
    // enable all context menu items for specified table-view
    ENABLED.remove( view );
  }

  /******************************************* disable *******************************************/
  /**
   * Removes specified optional menu items from the context menu for the specified table view.
   * If no configuration pre-exists, starts with all enabled and removes specified items.
   *
   * @param view the table view to configure
   * @param items the menu items to disable for the context menu
   */
  public static void disable( TableView view, TableMenuItems... items )
  {
    // disable specified context menu items for this table-view
    var set = ENABLED.get( view );
    if ( set != null )
      for ( var item : items )
        set.remove( item );
    else
    {
      // start with all enabled, then remove specified items
      set = EnumSet.allOf( TableMenuItems.class );
      for ( var item : items )
        set.remove( item );
      ENABLED.put( view, set );
    }
  }

  /****************************************** disableAll *****************************************/
  /**
   * Disables all optional menu items for the context menu for the specified table view.
   *
   * @param view the table view to configure
   */
  public static void disableAll( TableView view )
  {
    // disable all context menu items for this table-view
    var set = EnumSet.noneOf( TableMenuItems.class );
    ENABLED.put( view, set );
  }

  /****************************************** isEnabled ******************************************/
  /**
   * Checks whether a specific optional menu item is enabled for the given table view.
   * Defaults to true if no specific configuration exists for the view.
   *
   * @param view the table view to check
   * @param item the menu item to check
   * @return true if the item is enabled, false otherwise
   */
  public static boolean isEnabled( TableView view, TableMenuItems item )
  {
    // return true if specified context menu item is enabled for this table-view
    var set = ENABLED.get( view );
    return set == null || set.contains( item );
  }

}