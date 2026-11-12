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
 * Constructs menu contents for body, header, corner and trailing-space regions using
 * {@link #buildMenu(ContextMenu, TableView, int, int)}. Protected region methods and fluent item
 * methods support subclass customisation. Default body and corner regions add no items; headers
 * offer configured sort, filter, visibility and structural actions, and trailing space offers append.
 *
 * <p>The builder keeps mutable working state for the current build. Default callbacks capture their
 * own view, trigger coordinates and menu so later reuse cannot retarget existing items. Subclasses
 * installing deferred callbacks should likewise capture request-specific state. Selection and model
 * targets are resolved by the action when it runs; targeted callbacks first require their captured
 * row/column to remain valid and unhidden.</p>
 *
 * <p>Configuration and model-capability checks determine which items are added. This class neither
 * prepares selection nor shows a popup. Use sequentially on the JavaFX Application Thread.</p>
 */
public class TableContextMenuBuilder
{
  private TableView   m_view;       // the table view
  private int         m_triggerRow; // row view-index where context menu triggered
  private int         m_triggerCol; // column view-index where context menu triggered
  private ContextMenu m_menu;       // the context menu being built

  /******************************************* buildMenu *****************************************/
  /**
   * Sets the current build state and dispatches to the appropriate region hook. Either
   * {@link TableAxis#AFTER} coordinate selects trailing space; two {@link TableAxis#HEADER}
   * coordinates select the corner. Otherwise a header plus a body index selects that header,
   * and two body indexes select the body.
   *
   * <p>Does not clear existing menu items, change selection, show the popup or validate coordinates
   * against current axis counts. Region and item hooks use this build's working state.</p>
   *
   * @param menu the non-null menu to populate
   * @param view the non-null view supplying configuration, data and action targets
   * @param triggerRow the row view index or header/trailing-space sentinel
   * @param triggerCol the column view index or header/trailing-space sentinel
   * @return the region hook's result; default hooks return the supplied menu
   * @throws IllegalStateException if the coordinate combination does not identify a supported region
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
   * Adds append-row and append-column items for trailing space, subject to each item's configuration
   * and model capability. Uses the current build's menu and leaves any existing items in place.
   *
   * @return the constructed context menu
   */
  protected ContextMenu buildAfter()
  {
    // build append commands for space after the last table-view body row/column
    addAppendRow();
    addAppendColumn();
    return m_menu;
  }

  /****************************************** buildCorner ****************************************/
  /**
   * Handles the header intersection. The default implementation adds no items and returns the
   * current menu unchanged; subclasses may supply corner-specific contents.
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
   * Adds column-header filtering, sorting, Hide, Unhide, Insert and Delete in that order, subject
   * to each item's configuration and model capability.
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
   * Adds row-header filtering, sorting, Hide, Unhide, Insert and Delete in that order, subject
   * to each item's configuration and model capability.
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
   * Handles body-cell contents. The default implementation adds no items and returns the current
   * menu unchanged; subclasses may supply body-specific commands.
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
   * Adds an Unhide item delegating to {@link HideShow#showColumns(TableView)}, using action-time
   * selection. That action also reveals all hidden columns when every visible column is fully selected.
   * The item is added if {@link TableContextMenu.TableMenuItems#COLUMN_SHOW}
   * is enabled for this table view.
   *
   * @return this builder for method chaining
   */
  protected TableContextMenuBuilder addShowColumn()
  {
    // capture this request before a reusable builder constructs another menu
    var view = m_view;
    var menu = m_menu;

    // add a show column(s) menu item if option is enabled for this table-view
    if ( TableContextMenu.isEnabled( view, TableContextMenu.TableMenuItems.COLUMN_SHOW ) )
    {
      var item = new MenuItem( "Unhide" );
      item.setOnAction( event -> HideShow.showColumns( view ) );
      menu.getItems().add( item );
    }

    return this;
  }

  /****************************************** addShowRow *****************************************/
  /**
   * Adds an Unhide item delegating to {@link HideShow#showRows(TableView)}, using action-time
   * selection. That action also reveals all hidden rows when every visible row is fully selected.
   * The item is added if {@link TableContextMenu.TableMenuItems#ROW_SHOW}
   * is enabled for this table view.
   *
   * @return this builder for method chaining
   */
  protected TableContextMenuBuilder addShowRow()
  {
    // capture this request before a reusable builder constructs another menu
    var view = m_view;
    var menu = m_menu;

    // add a show rows menu item if option is enabled for this table-view
    if ( TableContextMenu.isEnabled( view, TableContextMenu.TableMenuItems.ROW_SHOW ) )
    {
      var item = new MenuItem( "Unhide" );
      item.setOnAction( event -> HideShow.showRows( view ) );
      menu.getItems().add( item );
    }

    return this;
  }

  /*************************************** addInsertColumn ***************************************/
  /**
   * Adds a menu item to insert a column before the triggered column (or before each selected
   * column if the triggered column is fully selected). Selection is resolved when the action runs;
   * a captured trigger that has become hidden or invalid makes the callback do nothing.
   * <p>
   * The item is added if {@link TableContextMenu.TableMenuItems#COLUMN_INSERT} is enabled for
   * this table view and the data model implements {@link IDataInsertDeleteColumns}.
   *
   * @return this builder for method chaining
   */
  protected TableContextMenuBuilder addInsertColumn()
  {
    // capture this request before a reusable builder constructs another menu
    var view = m_view;
    int column = m_triggerCol;
    var menu = m_menu;

    // add an insert column(s) menu item if option is enabled for this table-view
    if ( TableContextMenu.isEnabled( view, TableContextMenu.TableMenuItems.COLUMN_INSERT )
        && view.getData() instanceof IDataInsertDeleteColumns )
    {
      var item = new MenuItem( "Insert" );
      item.setOnAction( event ->
      {
        if ( view.getColumnsAxis().isVisible( column ) )
          Insert.insertColumns( view, column );
      } );
      menu.getItems().add( item );
    }

    return this;
  }

  /**************************************** addInsertRow *****************************************/
  /**
   * Adds a menu item to insert a row before the triggered row (or before each selected
   * row if the triggered row is fully selected). Selection is resolved when the action runs;
   * a captured trigger that has become hidden or invalid makes the callback do nothing.
   * <p>
   * The item is added if {@link TableContextMenu.TableMenuItems#ROW_INSERT} is enabled for
   * this table view and the data model implements {@link IDataInsertDeleteRows}.
   *
   * @return this builder for method chaining
   */
  protected TableContextMenuBuilder addInsertRow()
  {
    // capture this request before a reusable builder constructs another menu
    var view = m_view;
    int row = m_triggerRow;
    var menu = m_menu;

    // add an insert row(s) menu item if option is enabled for this table-view
    if ( TableContextMenu.isEnabled( view, TableContextMenu.TableMenuItems.ROW_INSERT )
        && view.getData() instanceof IDataInsertDeleteRows )
    {
      var item = new MenuItem( "Insert" );
      item.setOnAction( event ->
      {
        if ( view.getRowsAxis().isVisible( row ) )
          Insert.insertRows( view, row );
      } );
      menu.getItems().add( item );
    }

    return this;
  }

  /*************************************** addAppendColumn ***************************************/
  /**
   * Adds an item to append one column, using {@link #getAppendColumnLabel()} and the column count
   * at action time so a retained menu still appends at the current end.
   * <p>
   * The item is added if {@link TableContextMenu.TableMenuItems#COLUMN_INSERT} is enabled for
   * this table view and the data model implements {@link IDataInsertDeleteColumns}.
   *
   * @return this builder for method chaining
   */
  protected TableContextMenuBuilder addAppendColumn()
  {
    // capture this request before a reusable builder constructs another menu
    var view = m_view;
    var menu = m_menu;

    // add an insert column(s) menu item if option is enabled for this table-view
    if ( TableContextMenu.isEnabled( view, TableContextMenu.TableMenuItems.COLUMN_INSERT )
        && view.getData() instanceof IDataInsertDeleteColumns )
    {
      var item = new MenuItem( getAppendColumnLabel() );
      item.setOnAction( event -> Insert.insertColumns( view, view.getData().getColumnCount() ) );
      menu.getItems().add( item );
    }

    return this;
  }

  /**************************************** addAppendRow *****************************************/
  /**
   * Adds an item to append one row, using {@link #getAppendRowLabel()} and the row count at action
   * time so a retained menu still appends at the current end.
   * <p>
   * The item is added if {@link TableContextMenu.TableMenuItems#ROW_INSERT} is enabled for
   * this table view and the data model implements {@link IDataInsertDeleteRows}.
   *
   * @return this builder for method chaining
   */
  protected TableContextMenuBuilder addAppendRow()
  {
    // capture this request before a reusable builder constructs another menu
    var view = m_view;
    var menu = m_menu;

    // add an insert row(s) menu item if option is enabled for this table-view
    if ( TableContextMenu.isEnabled( view, TableContextMenu.TableMenuItems.ROW_INSERT )
        && view.getData() instanceof IDataInsertDeleteRows )
    {
      var item = new MenuItem( getAppendRowLabel() );
      item.setOnAction( event -> Insert.insertRows( view, view.getData().getRowCount() ) );
      menu.getItems().add( item );
    }

    return this;
  }

  /************************************* getAppendColumnLabel ************************************/
  /**
   * Supplies append-column wording without replacing capability checks or the undoable action.
   *
   * @return the item label; the default is {@code "Add column"}
   */
  protected String getAppendColumnLabel()
  {
    return "Add column";
  }

  /************************************** getAppendRowLabel **************************************/
  /**
   * Supplies append-row wording without replacing capability checks or the undoable action.
   *
   * @return the item label; the default is {@code "Add row"}
   */
  protected String getAppendRowLabel()
  {
    return "Add row";
  }

  /**************************************** addHideColumn ****************************************/
  /**
   * Adds an item to hide the captured trigger column, or the fully selected column group if the
   * trigger is fully selected at action time. A hidden or invalid trigger makes the callback do nothing.
   * The item is added if {@link TableContextMenu.TableMenuItems#COLUMN_HIDE}
   * is enabled for this table view.
   *
   * @return this builder for method chaining
   */
  protected TableContextMenuBuilder addHideColumn()
  {
    // capture this request before a reusable builder constructs another menu
    var view = m_view;
    int column = m_triggerCol;
    var menu = m_menu;

    // add a hide column(s) menu item if option is enabled for this table-view
    if ( TableContextMenu.isEnabled( view, TableContextMenu.TableMenuItems.COLUMN_HIDE ) )
    {
      var item = new MenuItem( "Hide" );
      item.setOnAction( event ->
      {
        if ( view.getColumnsAxis().isVisible( column ) )
          HideShow.hideColumns( view, column );
      } );
      menu.getItems().add( item );
    }

    return this;
  }

  /***************************************** addHideRow ******************************************/
  /**
   * Adds an item to hide the captured trigger row, or the fully selected row group if the trigger
   * is fully selected at action time. A hidden or invalid trigger makes the callback do nothing.
   * The item is added if {@link TableContextMenu.TableMenuItems#ROW_HIDE}
   * is enabled for this table view.
   *
   * @return this builder for method chaining
   */
  protected TableContextMenuBuilder addHideRow()
  {
    // capture this request before a reusable builder constructs another menu
    var view = m_view;
    int row = m_triggerRow;
    var menu = m_menu;

    // add a hide row(s) menu item if option is enabled for this table-view
    if ( TableContextMenu.isEnabled( view, TableContextMenu.TableMenuItems.ROW_HIDE ) )
    {
      var item = new MenuItem( "Hide" );
      item.setOnAction( event ->
      {
        if ( view.getRowsAxis().isVisible( row ) )
          HideShow.hideRows( view, row );
      } );
      menu.getItems().add( item );
    }

    return this;
  }

  /************************************* addFilterTextColumn *************************************/
  /**
   * Adds filtering of visible rows using the captured trigger column. Contains and Starts with
   * use case-sensitive text matching; Regex uses case-insensitive pattern matching. Each callback
   * requires the captured column to remain valid and unhidden.
   * The items are added if {@link TableContextMenu.TableMenuItems#COLUMN_FILTER_TEXT}
   * is enabled for this table view.
   *
   * @return this builder for method chaining
   */
  protected TableContextMenuBuilder addFilterTextColumn()
  {
    // capture this request before a reusable builder constructs another menu
    var view = m_view;
    int column = m_triggerCol;
    var menu = m_menu;

    // add a filter text sub-menu for column if option is enabled for this table-view
    if ( TableContextMenu.isEnabled( view, TableContextMenu.TableMenuItems.COLUMN_FILTER_TEXT ) )
    {
      var filterText = new Menu( "Filter text" );

      var contains = new MenuItemTextField( "Contains" );
      contains.setOnAction( event ->
      {
        if ( view.getColumnsAxis().isVisible( column ) )
          Filter.columnTextContains( view, column, contains.getFieldText(), true );
      } );

      var starts = new MenuItemTextField( "Starts with" );
      starts.setOnAction( event ->
      {
        if ( view.getColumnsAxis().isVisible( column ) )
          Filter.columnTextStarts( view, column, starts.getFieldText(), true );
      } );

      var regex = new MenuItemTextField( "Regex" );
      regex.setOnAction( event ->
      {
        if ( view.getColumnsAxis().isVisible( column ) )
          Filter.columnTextRegex( view, column, regex.getFieldText(), false );
      } );

      // ensure aligned when menu shown, and text-fill is correct (otherwise lost)
      filterText.setOnShown( event ->
      {
        var root = menu.getStyleableNode();
        var node = root == null ? null : root.lookup( ".label" );
        Paint textFill = node instanceof Label lbl ? lbl.getTextFill() : Color.BLACK;
        MenuItemTextField.align( textFill, contains, starts, regex );
      } );

      filterText.getItems().addAll( contains, starts, regex );
      menu.getItems().add( filterText );
    }

    return this;
  }

  /*************************************** addFilterTextRow **************************************/
  /**
   * Adds filtering of visible columns using the captured trigger row. Contains and Starts with
   * use case-sensitive text matching; Regex uses case-insensitive pattern matching. Each callback
   * requires the captured row to remain valid and unhidden.
   * The items are added if {@link TableContextMenu.TableMenuItems#ROW_FILTER_TEXT}
   * is enabled for this table view.
   *
   * @return this builder for method chaining
   */
  protected TableContextMenuBuilder addFilterTextRow()
  {
    // capture this request before a reusable builder constructs another menu
    var view = m_view;
    int row = m_triggerRow;
    var menu = m_menu;

    // add a filter text sub-menu for row if option is enabled for this table-view
    if ( TableContextMenu.isEnabled( view, TableContextMenu.TableMenuItems.ROW_FILTER_TEXT ) )
    {
      var filterText = new Menu( "Filter text" );

      var contains = new MenuItemTextField( "Contains" );
      contains.setOnAction( event ->
      {
        if ( view.getRowsAxis().isVisible( row ) )
          Filter.rowTextContains( view, row, contains.getFieldText(), true );
      } );

      var starts = new MenuItemTextField( "Starts with" );
      starts.setOnAction( event ->
      {
        if ( view.getRowsAxis().isVisible( row ) )
          Filter.rowTextStarts( view, row, starts.getFieldText(), true );
      } );

      var regex = new MenuItemTextField( "Regex" );
      regex.setOnAction( event ->
      {
        if ( view.getRowsAxis().isVisible( row ) )
          Filter.rowTextRegex( view, row, regex.getFieldText(), false );
      } );

      // ensure aligned when menu shown, and text-fill is correct (otherwise lost)
      filterText.setOnShown( event ->
      {
        var root = menu.getStyleableNode();
        var node = root == null ? null : root.lookup( ".label" );
        Paint textFill = node instanceof Label lbl ? lbl.getTextFill() : Color.BLACK;
        MenuItemTextField.align( textFill, contains, starts, regex );
      } );

      filterText.getItems().addAll( contains, starts, regex );
      menu.getItems().add( filterText );
    }

    return this;
  }

  /**************************************** addSortColumn ****************************************/
  /**
   * Adds ascending and descending sort items for visible rows, using the captured trigger column
   * as the sort key. A hidden or invalid key column makes the callback do nothing.
   * The items are added if {@link TableContextMenu.TableMenuItems#COLUMN_SORT}
   * is enabled for this table view.
   *
   * @return this builder for method chaining
   */
  protected TableContextMenuBuilder addSortColumn()
  {
    // capture this request before a reusable builder constructs another menu
    var view = m_view;
    int column = m_triggerCol;
    var menu = m_menu;

    // add sort menu items for column if option is enabled for this table-view
    if ( TableContextMenu.isEnabled( view, TableContextMenu.TableMenuItems.COLUMN_SORT ) )
    {
      var ascend = new MenuItem( "Sort ▲" );
      ascend.setOnAction( event ->
      {
        if ( view.getColumnsAxis().isVisible( column ) )
          Sort.columnSort( view, column, SortType.ASCENDING );
      } );

      var descend = new MenuItem( "Sort ▼" );
      descend.setOnAction( event ->
      {
        if ( view.getColumnsAxis().isVisible( column ) )
          Sort.columnSort( view, column, SortType.DESCENDING );
      } );

      menu.getItems().addAll( ascend, descend );
    }

    return this;
  }

  /****************************************** addSortRow *****************************************/
  /**
   * Adds ascending and descending sort items for visible columns, using the captured trigger row
   * as the sort key. A hidden or invalid key row makes the callback do nothing.
   * The items are added if {@link TableContextMenu.TableMenuItems#ROW_SORT}
   * is enabled for this table view.
   *
   * @return this builder for method chaining
   */
  protected TableContextMenuBuilder addSortRow()
  {
    // capture this request before a reusable builder constructs another menu
    var view = m_view;
    int row = m_triggerRow;
    var menu = m_menu;

    // add sort menu items for row if option is enabled for this table-view
    if ( TableContextMenu.isEnabled( view, TableContextMenu.TableMenuItems.ROW_SORT ) )
    {
      var ascend = new MenuItem( "Sort ◀" );
      ascend.setOnAction( event ->
      {
        if ( view.getRowsAxis().isVisible( row ) )
          Sort.rowSort( view, row, SortType.ASCENDING );
      } );

      var descend = new MenuItem( "Sort ▶" );
      descend.setOnAction( event ->
      {
        if ( view.getRowsAxis().isVisible( row ) )
          Sort.rowSort( view, row, SortType.DESCENDING );
      } );

      menu.getItems().addAll( ascend, descend );
    }

    return this;
  }

  /*************************************** addDeleteColumn ***************************************/
  /**
   * Adds an item to delete the captured trigger column, or the fully selected column group if the
   * trigger is fully selected at action time. A hidden or invalid trigger makes the callback do nothing.
   * <p>
   * The item is added if {@link TableContextMenu.TableMenuItems#COLUMN_DELETE} is enabled for
   * this table view and the model implements {@link IDataInsertDeleteColumns}.
   *
   * @return this builder for method chaining
   */
  protected TableContextMenuBuilder addDeleteColumn()
  {
    // capture this request before a reusable builder constructs another menu
    var view = m_view;
    int column = m_triggerCol;
    var menu = m_menu;

    // add a delete column(s) menu item if option is enabled for this table-view
    if ( TableContextMenu.isEnabled( view, TableContextMenu.TableMenuItems.COLUMN_DELETE )
        && view.getData() instanceof IDataInsertDeleteColumns )
    {
      var item = new MenuItem( "Delete" );
      item.setOnAction( event ->
      {
        if ( view.getColumnsAxis().isVisible( column ) )
          Delete.deleteColumns( view, column );
      } );
      menu.getItems().add( item );
    }

    return this;
  }

  /**************************************** addDeleteRow *****************************************/
  /**
   * Adds an item to delete the captured trigger row, or the fully selected row group if the trigger
   * is fully selected at action time. A hidden or invalid trigger makes the callback do nothing.
   * <p>
   * The item is added if {@link TableContextMenu.TableMenuItems#ROW_DELETE} is enabled for
   * this table view and the model implements {@link IDataInsertDeleteRows}.
   *
   * @return this builder for method chaining
   */
  protected TableContextMenuBuilder addDeleteRow()
  {
    // capture this request before a reusable builder constructs another menu
    var view = m_view;
    int row = m_triggerRow;
    var menu = m_menu;

    // add a delete row(s) menu item if option is enabled for this table-view
    if ( TableContextMenu.isEnabled( view, TableContextMenu.TableMenuItems.ROW_DELETE )
        && view.getData() instanceof IDataInsertDeleteRows )
    {
      var item = new MenuItem( "Delete" );
      item.setOnAction( event ->
      {
        if ( view.getRowsAxis().isVisible( row ) )
          Delete.deleteRows( view, row );
      } );
      menu.getItems().add( item );
    }

    return this;
  }
}
