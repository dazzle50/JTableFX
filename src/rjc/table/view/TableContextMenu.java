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
 * <p>Enabled-item configuration uses weak view keys. Each view owns its custom builder, so a
 * builder referring to its view does not keep that view alive through a static registry.</p>
 *
 * <p>Construction builds contents only; it does not prepare selection or show the popup. The normal
 * {@link TableView#openContextMenu(javafx.scene.input.ContextMenuEvent)} entry point owns those
 * interactions. Configuration changes affect subsequent construction, not existing menu items.
 * Enabled items remain subject to model capabilities and the selected builder's implementation.
 * Use on the JavaFX Application Thread.</p>
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
    /** Offer column hiding. */
    COLUMN_HIDE,
    /** Offer row hiding. */
    ROW_HIDE,
    /** Offer column unhiding. */
    COLUMN_SHOW,
    /** Offer row unhiding. */
    ROW_SHOW,
    /** Offer filtering of visible rows using a column's text. */
    COLUMN_FILTER_TEXT,
    /** Offer filtering of visible columns using a row's text. */
    ROW_FILTER_TEXT,
    /** Offer sorting of visible rows keyed by a column. */
    COLUMN_SORT,
    /** Offer sorting of visible columns keyed by a row. */
    ROW_SORT,
    /** Offer column insertion and appending when the model supports them. */
    COLUMN_INSERT,
    /** Offer row insertion and appending when the model supports them. */
    ROW_INSERT,
    /** Offer column deletion when the model supports it. */
    COLUMN_DELETE,
    /** Offer row deletion when the model supports it. */
    ROW_DELETE
  }

  private static final Map<TableView, Set<TableMenuItems>> ENABLED = new WeakHashMap<>(); // enabled items per view

  /***************************************** constructor *****************************************/
  /**
   * Constructs contents using the view's cached mouse row and column. Does not refresh hit testing
   * or resolve a keyboard target; use {@link #TableContextMenu(TableView, int, int)} when the target
   * has already been resolved independently of the pointer.
   *
   * <p>If a custom {@link TableContextMenuBuilder} has been registered for the view via
   * {@link #setBuilder}, that builder is used; otherwise the default builder is used.</p>
   *
   * @param view the table view for which to construct the context menu
   * @throws IllegalStateException if the builder cannot classify the cached mouse coordinates
   */
  public TableContextMenu( TableView view )
  {
    this( view, view.getMouseCell().getRow(), view.getMouseCell().getColumn() );
  }

  /***************************************** constructor *****************************************/
  /**
   * Constructs contents for explicit view coordinates through the view's configured builder, or
   * a fresh default builder if none is registered. Leaves selection and popup display unchanged.
   *
   * @param view the view providing menu configuration and command targets
   * @param viewRow the body row index or a {@link rjc.table.view.axis.TableAxis} region sentinel
   * @param viewColumn the body column index or a {@link rjc.table.view.axis.TableAxis} region sentinel
   * @throws IllegalStateException if the builder cannot classify the supplied coordinate region
   */
  public TableContextMenu( TableView view, int viewRow, int viewColumn )
  {
    var builder = view.m_contextMenuBuilder;
    if ( builder == null )
      builder = new TableContextMenuBuilder();
    builder.buildMenu( this, view, viewRow, viewColumn );
  }

  /***************************************** setBuilder ******************************************/
  /**
   * Registers a custom {@link TableContextMenuBuilder} for the specified table view,
   * overriding default construction for subsequent menus. The view retains this configuration
   * through reset. Supplying null restores the default builder, as does {@link #removeBuilder}.
   *
   * @param view    the table view to configure
   * @param builder the custom builder, or null to use the default
   */
  public static void setBuilder( TableView view, TableContextMenuBuilder builder )
  {
    view.m_contextMenuBuilder = builder;
  }

  /**************************************** removeBuilder ****************************************/
  /**
   * Removes any custom {@link TableContextMenuBuilder} registered for the specified table view,
   * restoring default construction for subsequent menus. Does not change enabled-item configuration
   * or menus that have already been constructed.
   *
   * @param view the table view whose custom builder should be removed
   */
  public static void removeBuilder( TableView view )
  {
    view.m_contextMenuBuilder = null;
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
   * This query does not test model capability, selection applicability or target validity.
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
