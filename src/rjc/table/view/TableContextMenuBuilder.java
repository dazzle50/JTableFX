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
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import rjc.table.data.IDataInsertDeleteColumns;
import rjc.table.data.IDataInsertDeleteRows;
import rjc.table.view.action.Delete;
import rjc.table.view.action.Filter;
import rjc.table.view.action.HideShow;
import rjc.table.view.action.Insert;
import rjc.table.view.action.Sort;
import rjc.table.view.action.Sort.SortType;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/**************** Builder for constructing context menus for table-view cells ********************/
/*************************************************************************************************/

/**
 * Constructs context menus with appropriate items based on the location
 * of the triggering cell (header, body, corner, etc.). Provides fluent API
 * for adding menu items such as sort, filter, hide/show, delete, and clipboard operations.
 */
public class TableContextMenuBuilder
{
  private TableView   m_view;       // the table view
  private int         m_triggerRow; // row view-index where context menu triggered
  private int         m_triggerCol; // column view-index where context menu triggered
  private ContextMenu m_menu;       // the context menu being built

  /********************************************* build *******************************************/
  /**
   * Orchestrates the construction of the context menu based on the cell coordinates
   * provided during instantiation.
   *
   * @return the constructed context menu
   * @throws IllegalStateException if the mouse coordinates do not map to a valid region
   */
  public ContextMenu buildMenu( ContextMenu menu, TableView view, int triggerRow, int triggerCol )
  {
    // initialise builder with menu, table view and mouse cell coordinates
    m_menu = menu;
    m_view = view;
    m_triggerRow = triggerRow;
    m_triggerCol = triggerCol;

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
  protected ContextMenu buildAfter()
  {
    // build context menu for after last table-view body row/column - currently no context menu
    addAppendRow();
    addAppendColumn();
    return m_menu;
  }

  /****************************************** buildCorner ****************************************/
  /**
   * Builds context menu for the table-view corner cell (intersection of headers).
   *
   * @return the constructed context menu
   */
  protected ContextMenu buildCorner()
  {
    // build context menu for table-view corner cell - currently no context menu
    // addTODO( "corner" );
    return m_menu;
  }

  /*************************************** buildColumnHeader *************************************/
  /**
   * Builds context menu for table-view column header cells.
   *
   * @return the constructed context menu
   */
  protected ContextMenu buildColumnHeader()
  {
    // build context menu for table-view column header row - add relevant menu items
    addFilterTextColumn().addSortColumn().addHideColumn().addShowColumn().addInsertColumn().addDeleteColumn();
    return m_menu;
  }

  /**************************************** buildRowHeader ***************************************/
  /**
   * Builds context menu for table-view row header cells.
   *
   * @return the constructed context menu
   */
  protected ContextMenu buildRowHeader()
  {
    // build context menu for table-view row header column - add relevant menu items
    addFilterTextRow().addSortRow().addHideRow().addShowRow().addInsertRow().addDeleteRow();
    return m_menu;
  }

  /******************************************* buildBody *****************************************/
  /**
   * Builds context menu for table-view body cells (data cells).
   *
   * @return the constructed context menu
   */
  protected ContextMenu buildBody()
  {
    // build context menu for table-view body cells - currently no context menu
    // addTODO( "body" );
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
      var item = new MenuItem( "Unhide" );
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
      var item = new MenuItem( "Unhide" );
      item.setOnAction( event -> HideShow.showRows( m_view ) );
      m_menu.getItems().add( item );
    }

    return this;
  }

  /*************************************** addInsertColumn ***************************************/
  /**
   * Adds a menu item to insert a column before the triggered column (or before each selected
   * column if the triggered column is selected).
   * <p>
   * The item is added if {@link TableContextMenu.TableMenuItems#COLUMN_INSERT} is enabled for
   * this table view and the data model implements {@link IDataInsertDeleteColumns}.
   *
   * @return this builder for method chaining
   */
  protected TableContextMenuBuilder addInsertColumn()
  {
    // add an insert column(s) menu item if option is enabled for this table-view
    if ( TableContextMenu.isEnabled( m_view, TableContextMenu.TableMenuItems.COLUMN_INSERT )
        && m_view.getData() instanceof IDataInsertDeleteColumns )
    {
      var item = new MenuItem( "Insert" );
      item.setOnAction( event -> Insert.insertColumns( m_view, m_triggerCol ) );
      m_menu.getItems().add( item );
    }

    return this;
  }

  /**************************************** addInsertRow *****************************************/
  /**
   * Adds a menu item to insert a row before the triggered row (or before each selected
   * row if the triggered row is selected).
   * <p>
   * The item is added if {@link TableContextMenu.TableMenuItems#ROW_INSERT} is enabled for
   * this table view and the data model implements {@link IDataInsertDeleteRows}.
   *
   * @return this builder for method chaining
   */
  protected TableContextMenuBuilder addInsertRow()
  {
    // add an insert row(s) menu item if option is enabled for this table-view
    if ( TableContextMenu.isEnabled( m_view, TableContextMenu.TableMenuItems.ROW_INSERT )
        && m_view.getData() instanceof IDataInsertDeleteRows )
    {
      var item = new MenuItem( "Insert" );
      item.setOnAction( event -> Insert.insertRows( m_view, m_triggerRow ) );
      m_menu.getItems().add( item );
    }

    return this;
  }

  /*************************************** addAppendColumn ***************************************/
  /**
   * Adds a menu item to append a column to the end of table.
   * <p>
   * The item is added if {@link TableContextMenu.TableMenuItems#COLUMN_INSERT} is enabled for
   * this table view and the data model implements {@link IDataInsertDeleteColumns}.
   *
   * @return this builder for method chaining
   */
  protected TableContextMenuBuilder addAppendColumn()
  {
    // add an insert column(s) menu item if option is enabled for this table-view
    if ( TableContextMenu.isEnabled( m_view, TableContextMenu.TableMenuItems.COLUMN_INSERT )
        && m_view.getData() instanceof IDataInsertDeleteColumns )
    {
      var item = new MenuItem( "Add column" );
      item.setOnAction( event -> Insert.insertColumns( m_view, m_view.getData().getColumnCount() ) );
      m_menu.getItems().add( item );
    }

    return this;
  }

  /**************************************** addAppendRow *****************************************/
  /**
   * Adds a menu item to append a row to the end of table.
   * <p>
   * The item is added if {@link TableContextMenu.TableMenuItems#ROW_INSERT} is enabled for
   * this table view and the data model implements {@link IDataInsertDeleteRows}.
   *
   * @return this builder for method chaining
   */
  protected TableContextMenuBuilder addAppendRow()
  {
    // add an insert row(s) menu item if option is enabled for this table-view
    if ( TableContextMenu.isEnabled( m_view, TableContextMenu.TableMenuItems.ROW_INSERT )
        && m_view.getData() instanceof IDataInsertDeleteRows )
    {
      var item = new MenuItem( "Add row" );
      item.setOnAction( event -> Insert.insertRows( m_view, m_view.getData().getRowCount() ) );
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
        var node = m_menu.getStyleableNode().lookup( ".label" );
        Paint textFill = node instanceof Label lbl ? lbl.getTextFill() : Color.BLACK;
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
        var node = m_menu.getStyleableNode().lookup( ".label" );
        Paint textFill = node instanceof Label lbl ? lbl.getTextFill() : Color.BLACK;
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
      var ascend = new MenuItem( "Sort ▲" );
      ascend.setOnAction( event -> Sort.columnSort( m_view, m_triggerCol, SortType.ASCENDING ) );

      var descend = new MenuItem( "Sort ▼" );
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
      var ascend = new MenuItem( "Sort ◀" );
      ascend.setOnAction( event -> Sort.rowSort( m_view, m_triggerRow, SortType.ASCENDING ) );

      var descend = new MenuItem( "Sort ▶" );
      descend.setOnAction( event -> Sort.rowSort( m_view, m_triggerRow, SortType.DESCENDING ) );

      m_menu.getItems().addAll( ascend, descend );
    }

    return this;
  }

  /*************************************** addDeleteColumn ***************************************/
  /**
   * Adds a menu item to delete the selected column(s).
   * <p>
   * The item is added if {@link TableContextMenu.TableMenuItems#COLUMN_DELETE} is enabled for
   * this table view.
   *
   * @return this builder for method chaining
   */
  protected TableContextMenuBuilder addDeleteColumn()
  {
    // add a delete column(s) menu item if option is enabled for this table-view
    if ( TableContextMenu.isEnabled( m_view, TableContextMenu.TableMenuItems.COLUMN_DELETE )
        && m_view.getData() instanceof IDataInsertDeleteColumns )
    {
      var item = new MenuItem( "Delete" );
      item.setOnAction( event -> Delete.deleteColumns( m_view, m_triggerCol ) );
      m_menu.getItems().add( item );
    }

    return this;
  }

  /**************************************** addDeleteRow *****************************************/
  /**
   * Adds a menu item to delete the selected row(s).
   * <p>
   * The item is added if {@link TableContextMenu.TableMenuItems#ROW_DELETE} is enabled for
   * this table view.
   *
   * @return this builder for method chaining
   */
  protected TableContextMenuBuilder addDeleteRow()
  {
    // add a delete row(s) menu item if option is enabled for this table-view
    if ( TableContextMenu.isEnabled( m_view, TableContextMenu.TableMenuItems.ROW_DELETE )
        && m_view.getData() instanceof IDataInsertDeleteRows )
    {
      var item = new MenuItem( "Delete" );
      item.setOnAction( event -> Delete.deleteRows( m_view, m_triggerRow ) );
      m_menu.getItems().add( item );
    }

    return this;
  }
}