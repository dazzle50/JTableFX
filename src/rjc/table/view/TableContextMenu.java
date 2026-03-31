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
/****************************** Default context-menu for table-view ******************************/
/*************************************************************************************************/

/**
 * Default context menu for a {@link TableView}, providing optional items for hiding, showing,
 * filtering, sorting, inserting, and deleting rows and columns.
 *
 * <p>Optional items are enabled or disabled per view via the static {@link #enable},
 * {@link #disable}, {@link #enableAll}, and {@link #disableAll} methods. A custom
 * {@link TableContextMenuBuilder} may be registered per view via {@link #setBuilder} to
 * override the default menu-construction behaviour.</p>
 *
 * <p>Configuration is held in {@link WeakHashMap}s keyed on {@link TableView}, so entries
 * are automatically released when a view is garbage-collected.</p>
 */
public class TableContextMenu extends ContextMenu
{
  /**
   * Optional items that may appear in the context menu.
   * Each item may be independently enabled or disabled per table view.
   */
  // optional context menu items that can be enabled/disabled for context menu
  public enum TableMenuItems
  {
    COLUMN_HIDE, ROW_HIDE, COLUMN_SHOW, ROW_SHOW, COLUMN_FILTER_TEXT, ROW_FILTER_TEXT, COLUMN_SORT, ROW_SORT, COLUMN_INSERT, ROW_INSERT, COLUMN_DELETE, ROW_DELETE
  }

  private static final Map<TableView, Set<TableMenuItems>>     ENABLED = new WeakHashMap<>();
  private static final Map<TableView, TableContextMenuBuilder> BUILDER = new WeakHashMap<>();

  /***************************************** constructor *****************************************/
  /**
   * Constructs the context menu for the given table view, building it for the cell under the
   * mouse pointer at the time the menu was triggered.
   *
   * <p>If a custom {@link TableContextMenuBuilder} has been registered for the view via
   * {@link #setBuilder}, that builder is used; otherwise the default builder is used.</p>
   *
   * @param view the table view for which to construct the context menu
   */
  public TableContextMenu( TableView view )
  {
    // get the cell coordinates where the context menu was triggered
    int mouseRow = view.getMouseCell().getRow();
    int mouseCol = view.getMouseCell().getColumn();

    // use builder to construct the appropriate menu for the location
    var builder = BUILDER.getOrDefault( view, new TableContextMenuBuilder() );
    builder.buildMenu( this, view, mouseRow, mouseCol );
  }

  /***************************************** setBuilder ******************************************/
  /**
   * Registers a custom {@link TableContextMenuBuilder} for the specified table view,
   * overriding the default menu-construction behaviour.
   *
   * @param view    the table view to configure
   * @param builder the builder to use when constructing the context menu
   */
  public static void setBuilder( TableView view, TableContextMenuBuilder builder )
  {
    BUILDER.put( view, builder );
  }

  /**************************************** removeBuilder ****************************************/
  /**
   * Removes any custom {@link TableContextMenuBuilder} registered for the specified table view,
   * restoring the default menu-construction behaviour.
   *
   * @param view the table view whose custom builder should be removed
   */
  public static void removeBuilder( TableView view )
  {
    BUILDER.remove( view );
  }

  /******************************************* enable ********************************************/
  /**
   * Enables the specified optional menu items for the given table view.
   *
   * <p>This method only has effect if a prior call to {@link #disable} or {@link #disableAll}
   * has established an explicit configuration for the view. If no such configuration exists
   * (i.e. all items are already enabled by default), this call is a no-op.</p>
   *
   * @param view  the table view to configure
   * @param items the menu items to enable
   */
  public static void enable( TableView view, TableMenuItems... items )
  {
    // enable specified context menu items for this table-view
    var set = ENABLED.get( view );
    if ( set != null )
      for ( var item : items )
        set.add( item );
  }

  /****************************************** enableAll ******************************************/
  /**
   * Enables all optional menu items for the context menu for the specified table view by
   * removing any explicit configuration, restoring the default behaviour where all items
   * are considered enabled.
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
   * Disables the specified optional menu items for the context menu for the given table view.
   *
   * <p>If no explicit configuration exists for the view, one is created initialised with all
   * items enabled, and then the specified items are removed.</p>
   *
   * @param view  the table view to configure
   * @param items the menu items to disable
   */
  public static void disable( TableView view, TableMenuItems... items )
  {
    // disable specified context menu items for this table-view
    var set = ENABLED.get( view );
    if ( set == null )
    {
      set = EnumSet.allOf( TableMenuItems.class );
      ENABLED.put( view, set );
    }

    for ( var item : items )
      set.remove( item );
  }

  /***************************************** disableAll ******************************************/
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
   * Returns whether the specified optional menu item is enabled for the given table view.
   * Defaults to {@code true} if no explicit configuration has been set for the view.
   *
   * @param view the table view to query
   * @param item the menu item to check
   * @return {@code true} if the item is enabled; {@code false} otherwise
   */
  public static boolean isEnabled( TableView view, TableMenuItems item )
  {
    // return true if specified context menu item is enabled for this table-view
    var set = ENABLED.get( view );
    return set == null || set.contains( item );
  }

}