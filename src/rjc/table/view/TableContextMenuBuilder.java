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

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.paint.Paint;
import rjc.table.view.action.Filter;
import rjc.table.view.action.HideShow;
import rjc.table.view.action.Sort;
import rjc.table.view.action.Sort.SortType;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/**************** Builder for constructing context menus for table-view cells ********************/
/*************************************************************************************************/

/**
 * Constructs context menus with appropriate items based on the location
 * of the triggering cell (header, body, corner, etc.). Provides fluent API
 * for adding menu items such as sort, filter, hide/show, and clipboard operations.
 */
public class TableContextMenuBuilder
{
  private TableView   m_view;       // the table view
  private int         m_triggerRow; // row view-index where context menu triggered
  private int         m_triggerCol; // column view-index where context menu triggered
  private ContextMenu m_menu;       // the context menu being built

  /****************************************** constructor *****************************************/
  /**
   * Creates a new context menu builder for the specified table view and cell location.
   *
   * @param view the table view the menu is for
   * @param triggerRow the row index where the context menu was triggered
   * @param triggerCol the column index where the context menu was triggered
   */
  public TableContextMenuBuilder( TableView view, int triggerRow, int triggerCol )
  {
    // initialise builder with table view and mouse cell coordinates
    m_view = view;
    m_triggerRow = triggerRow;
    m_triggerCol = triggerCol;
    m_menu = new ContextMenu();
  }

  /********************************************* build *******************************************/
  /**
   * Orchestrates the construction of the context menu based on the cell coordinates 
   * provided during instantiation.
   *
   * @return the constructed context menu
   * @throws IllegalStateException if the mouse coordinates do not map to a valid region
   */
  public ContextMenu build()
  {
    // determine which type of menu to build based on cell location
    if ( m_triggerRow == TableAxis.AFTER || m_triggerCol == TableAxis.AFTER )
      return buildAfter();
    if ( m_triggerRow == TableAxis.HEADER && m_triggerCol == TableAxis.HEADER )
      return buildCorner();
    if ( m_triggerRow == TableAxis.HEADER && m_triggerCol >= TableAxis.FIRSTCELL )
      return buildColumnHeader();
    if ( m_triggerCol == TableAxis.HEADER && m_triggerRow >= TableAxis.FIRSTCELL )
      return buildRowHeader();
    if ( m_triggerRow >= TableAxis.FIRSTCELL && m_triggerCol >= TableAxis.FIRSTCELL )
      return buildBody();

    throw new IllegalStateException( "Unexpected mouse cell for context menu " + m_triggerCol + "," + m_triggerRow );
  }

  /****************************************** buildAfter *****************************************/
  /**
   * Builds context menu for cells after the last table-view body row/column.
   *
   * @return the constructed context menu
   */
  public ContextMenu buildAfter()
  {
    // build context menu for after last table-view body row/column - currently no context menu
    return m_menu;
  }

  /****************************************** buildCorner ****************************************/
  /**
   * Builds context menu for the table-view corner cell (intersection of headers).
   *
   * @return the constructed context menu
   */
  public ContextMenu buildCorner()
  {
    // build context menu for table-view corner cell - currently no context menu
    addTODO( "corner" );
    return m_menu;
  }

  /*************************************** buildColumnHeader *************************************/
  /**
   * Builds context menu for table-view column header cells.
   *
   * @return the constructed context menu
   */
  public ContextMenu buildColumnHeader()
  {
    // build context menu for table-view column header row - add relevant menu items
    addFilterTextColumn().addSortColumn().addHideColumn().addShowColumn();
    return m_menu;
  }

  /**************************************** buildRowHeader ***************************************/
  /**
   * Builds context menu for table-view row header cells.
   *
   * @return the constructed context menu
   */
  public ContextMenu buildRowHeader()
  {
    // build context menu for table-view row header column - add relevant menu items
    addFilterTextRow().addSortRow().addHideRow().addShowRow();
    return m_menu;
  }

  /******************************************* buildBody *****************************************/
  /**
   * Builds context menu for table-view body cells (data cells).
   *
   * @return the constructed context menu
   */
  public ContextMenu buildBody()
  {
    // build context menu for table-view body cells - currently no context menu
    addTODO( "body" );
    return m_menu;
  }

  /******************************************* addTODO *******************************************/
  /**
   * Adds a disabled TODO placeholder menu item with the specified name.
   * Used to indicate menu options that are planned but not yet implemented.
   *
   * @param name the name to display in the TODO item
   * @return this builder for method chaining
   */
  protected TableContextMenuBuilder addTODO( String name )
  {
    // add a disabled TODO menu item
    var item = new MenuItem( "TODO " + name );
    item.setDisable( true );
    m_menu.getItems().add( item );
    return this;
  }

  /***************************************** addSeparator ****************************************/
  /**
   * Adds a visual separator between menu items.
   *
   * @return this builder for method chaining
   */
  protected TableContextMenuBuilder addSeparator()
  {
    // add a separator menu item
    m_menu.getItems().add( new SeparatorMenuItem() );
    return this;
  }

  /**************************************** addShowColumn ****************************************/
  /**
   * Adds a menu item to show hidden columns.
   * The item is added if {@link TableContextMenu.TableMenuItems#COLUMN_SHOW}
   * is enabled for this table view.
   *
   * @return this builder for method chaining
   */
  protected TableContextMenuBuilder addShowColumn()
  {
    // add a show column(s) menu item if option is enabled for this table-view
    if ( TableContextMenu.isEnabled( m_view, TableContextMenu.TableMenuItems.COLUMN_SHOW ) )
    {
      var item = new MenuItem( "Show" );
      item.setOnAction( event -> HideShow.showColumns( m_view ) );
      m_menu.getItems().add( item );
    }

    return this;
  }

  /****************************************** addShowRow *****************************************/
  /**
   * Adds a menu item to show hidden rows.
   * The item is added if {@link TableContextMenu.TableMenuItems#ROW_SHOW}
   * is enabled for this table view.
   *
   * @return this builder for method chaining
   */
  protected TableContextMenuBuilder addShowRow()
  {
    // add a show rows menu item if option is enabled for this table-view
    if ( TableContextMenu.isEnabled( m_view, TableContextMenu.TableMenuItems.ROW_SHOW ) )
    {
      var item = new MenuItem( "Show" );
      item.setOnAction( event -> HideShow.showRows( m_view ) );
      m_menu.getItems().add( item );
    }

    return this;
  }

  /**************************************** addHideColumn ****************************************/
  /**
   * Adds a menu item to hide the selected column(s).
   * The item is added if {@link TableContextMenu.TableMenuItems#COLUMN_HIDE}
   * is enabled for this table view.
   *
   * @return this builder for method chaining
   */
  protected TableContextMenuBuilder addHideColumn()
  {
    // add a hide column(s) menu item if option is enabled for this table-view
    if ( TableContextMenu.isEnabled( m_view, TableContextMenu.TableMenuItems.COLUMN_HIDE ) )
    {
      var item = new MenuItem( "Hide" );
      item.setOnAction( event -> HideShow.hideColumns( m_view, m_triggerCol ) );
      m_menu.getItems().add( item );
    }

    return this;
  }

  /***************************************** addHideRow ******************************************/
  /**
   * Adds a menu item to hide the selected row(s).
   * The item is added if {@link TableContextMenu.TableMenuItems#ROW_HIDE}
   * is enabled for this table view.
   *
   * @return this builder for method chaining
   */
  protected TableContextMenuBuilder addHideRow()
  {
    // add a hide row(s) menu item if option is enabled for this table-view
    if ( TableContextMenu.isEnabled( m_view, TableContextMenu.TableMenuItems.ROW_HIDE ) )
    {
      var item = new MenuItem( "Hide" );
      item.setOnAction( event -> HideShow.hideRows( m_view, m_triggerRow ) );
      m_menu.getItems().add( item );
    }

    return this;
  }

  /************************************* addFilterTextColumn *************************************/
  /**
   * Adds a submenu with text filtering options for the column.
   * Includes "Contains", "Starts with", and "Regex" filter types.
   * The items are added if {@link TableContextMenu.TableMenuItems#COLUMN_FILTER_TEXT}
   * is enabled for this table view.
   *
   * @return this builder for method chaining
   */
  protected TableContextMenuBuilder addFilterTextColumn()
  {
    // add a filter text sub-menu for column if option is enabled for this table-view
    if ( TableContextMenu.isEnabled( m_view, TableContextMenu.TableMenuItems.COLUMN_FILTER_TEXT ) )
    {
      var filterText = new Menu( "Filter text" );

      var contains = new MenuItemTextField( "Contains" );
      contains
          .setOnAction( ( event ) -> Filter.columnTextContains( m_view, m_triggerCol, contains.getFieldText(), true ) );

      var starts = new MenuItemTextField( "Starts with" );
      starts.setOnAction( ( event ) -> Filter.columnTextStarts( m_view, m_triggerCol, starts.getFieldText(), true ) );

      var regex = new MenuItemTextField( "Regex" );
      regex.setOnAction( ( event ) -> Filter.columnTextRegex( m_view, m_triggerCol, regex.getFieldText(), false ) );

      // ensure aligned when menu shown, and text-fill is correct (otherwise lost)
      filterText.setOnShown( event ->
      {
        Paint textFill = ( (Label) m_menu.getStyleableNode().lookup( ".label" ) ).getTextFill();
        MenuItemTextField.align( textFill, contains, starts, regex );
      } );

      filterText.getItems().addAll( contains, starts, regex );
      m_menu.getItems().add( filterText );
    }

    return this;
  }

  /*************************************** addFilterTextRow **************************************/
  /**
   * Adds a submenu with text filtering options for the row.
   * Includes "Contains", "Starts with", and "Regex" filter types.
   * The items are added if {@link TableContextMenu.TableMenuItems#ROW_FILTER_TEXT}
   * is enabled for this table view. 
   *
   * @return this builder for method chaining
   */
  protected TableContextMenuBuilder addFilterTextRow()
  {
    // add a filter text sub-menu for row if option is enabled for this table-view
    if ( TableContextMenu.isEnabled( m_view, TableContextMenu.TableMenuItems.ROW_FILTER_TEXT ) )
    {
      var filterText = new Menu( "Filter text" );

      var contains = new MenuItemTextField( "Contains" );
      contains
          .setOnAction( ( event ) -> Filter.rowTextContains( m_view, m_triggerRow, contains.getFieldText(), true ) );

      var starts = new MenuItemTextField( "Starts with" );
      starts.setOnAction( ( event ) -> Filter.rowTextStarts( m_view, m_triggerRow, starts.getFieldText(), true ) );

      var regex = new MenuItemTextField( "Regex" );
      regex.setOnAction( ( event ) -> Filter.rowTextRegex( m_view, m_triggerRow, regex.getFieldText(), false ) );

      // ensure aligned when menu shown, and text-fill is correct (otherwise lost)
      filterText.setOnShown( event ->
      {
        Paint textFill = ( (Label) m_menu.getStyleableNode().lookup( ".label" ) ).getTextFill();
        MenuItemTextField.align( textFill, contains, starts, regex );
      } );

      filterText.getItems().addAll( contains, starts, regex );
      m_menu.getItems().add( filterText );
    }

    return this;
  }

  /**************************************** addSortColumn ****************************************/
  /**
   * Adds menu items to sort the column in ascending or descending order.
   * The items are added if {@link TableContextMenu.TableMenuItems#COLUMN_SORT}
   * is enabled for this table view.
   * 
   * @return this builder for method chaining
   */
  protected TableContextMenuBuilder addSortColumn()
  {
    // add sort menu items for column if option is enabled for this table-view
    if ( TableContextMenu.isEnabled( m_view, TableContextMenu.TableMenuItems.COLUMN_SORT ) )
    {
      var ascend = new MenuItem( "Sort ↓" );
      ascend.setOnAction( event -> Sort.columnSort( m_view, m_triggerCol, SortType.ASCENDING ) );

      var descend = new MenuItem( "Sort ↑" );
      descend.setOnAction( event -> Sort.columnSort( m_view, m_triggerCol, SortType.DESCENDING ) );

      m_menu.getItems().addAll( ascend, descend );
    }

    return this;
  }

  /****************************************** addSortRow *****************************************/
  /**
   * Adds menu items to sort the row in ascending or descending order.
   * The items are added if {@link TableContextMenu.TableMenuItems#ROW_SORT}
   * is enabled for this table view.
   * 
   * @return this builder for method chaining
   */
  protected TableContextMenuBuilder addSortRow()
  {
    // add sort menu items for row if option is enabled for this table-view
    if ( TableContextMenu.isEnabled( m_view, TableContextMenu.TableMenuItems.ROW_SORT ) )
    {
      var ascend = new MenuItem( "Sort →" );
      ascend.setOnAction( event -> Sort.rowSort( m_view, m_triggerRow, SortType.ASCENDING ) );

      var descend = new MenuItem( "Sort ←" );
      descend.setOnAction( event -> Sort.rowSort( m_view, m_triggerRow, SortType.DESCENDING ) );

      m_menu.getItems().addAll( ascend, descend );
    }

    return this;
  }

}