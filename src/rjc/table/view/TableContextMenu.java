/**************************************************************************
 *  Copyright (C) 2025 by Richard Crook                                   *
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

import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import rjc.table.view.action.Filter;
import rjc.table.view.action.HideShow;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/************************ Context menu for table-view header & body cells ************************/
/*************************************************************************************************/

/**
 * Custom context menu that provides contextual actions for table cells.
 * Displays different menu items based on whether the right-click occurred
 * on header cells or body cells, offering operations like insert, delete,
 * hide/unhide, filter, sort, and clipboard operations.
 */
public class TableContextMenu extends ContextMenu
{
  private TableView        m_view;                          // the table view
  private int              m_mouseRow;                      // row index where context menu triggered
  private int              m_mouseCol;                      // column index where context menu triggered

  private static final int FIRSTCELL = TableAxis.FIRSTCELL; // index of first body row/column
  private static final int HEADER    = TableAxis.HEADER;    // index of header row/column
  private static final int AFTER     = TableAxis.AFTER;     // index of after-last row/column

  // optional context menu items that can be omitted from context menu
  public enum OptionalMenuItems
  {
    HIDE_COLUMNS, HIDE_ROWS, SHOW_ALL_COLUMNS, SHOW_ALL_ROWS, FILTER_COLUMN_CONTAINS_TEXT
  }

  private static final Map<TableView, Set<OptionalMenuItems>> OMIT = new WeakHashMap<>();

  /******************************************** show *********************************************/
  public static void show( TableView view, double x, double y )
  {
    // get the cell coordinates where the context menu was triggered
    int mouseRow = view.getMouseCell().getRow();
    int mouseCol = view.getMouseCell().getColumn();

    // generate the context-menu and show (if has any items)
    var menu = new TableContextMenu( view, mouseRow, mouseCol );
    if ( !menu.getItems().isEmpty() )
      menu.show( view.getScene().getWindow(), x, y );
  }

  /******************************************* omit **********************************************/
  public static void omit( TableView view, OptionalMenuItems... items )
  {
    // omit specified context menu items for this table-view
    var set = OMIT.get( view );
    if ( set == null )
    {
      set = EnumSet.noneOf( OptionalMenuItems.class );
      OMIT.put( view, set );
    }
    for ( var item : items )
      set.add( item );
  }

  /***************************************** clearOmit *******************************************/
  public static void clearOmit( TableView view )
  {
    // clear omitted context menu items for this table-view
    OMIT.remove( view );
  }

  /***************************************** isOmitted *******************************************/
  public static boolean isOmitted( TableView view, OptionalMenuItems item )
  {
    // return true if specified context menu item is omitted for this table-view
    var set = OMIT.get( view );
    return set != null && set.contains( item );
  }

  /**************************************** constructor ******************************************/
  private TableContextMenu( TableView view, int mouseRow, int mouseCol )
  {
    // prepare initially empty context menu
    m_view = view;
    m_mouseRow = mouseRow;
    m_mouseCol = mouseCol;

    // add menu items depending on where context menu was triggered
    if ( mouseRow == AFTER || mouseCol == AFTER )
      buildAfter();
    else if ( mouseRow == HEADER && mouseCol == HEADER )
      buildCorner();
    else if ( mouseRow == HEADER && mouseCol >= FIRSTCELL )
      buildColumnHeader();
    else if ( mouseCol == HEADER && mouseRow >= FIRSTCELL )
      buildRowHeader();
    else if ( mouseRow >= FIRSTCELL && mouseCol >= FIRSTCELL )
      buildBody();
    else
      throw new IllegalStateException( "Unexpected mouse cell for context menu " + mouseCol + "," + mouseRow );
  }

  // ####################################### Build Methods #######################################

  /***************************************** buildAfter ******************************************/
  private void buildAfter()
  {
    // build context menu for after last table-view body row/column - currently no context menu
  }

  /***************************************** buildCorner *****************************************/
  private void buildCorner()
  {
    // build context menu for table-view corner cell - currently no context menu
    addTODO( "corner" );
  }

  /************************************** buildColumnHeader **************************************/
  private void buildColumnHeader()
  {
    // build context menu for table-view column header row - add relevant menu items
    addHideColumn().addFilterColumn().addSeparator().addShowAllColumns();
  }

  /*************************************** buildRowHeader ****************************************/
  private void buildRowHeader()
  {
    // build context menu for table-view row header column - add relevant menu items
    addHideRow().addSeparator().addShowAllRows();
  }

  /****************************************** buildBody ******************************************/
  private void buildBody()
  {
    // build context menu for table-view body cells - currently no context menu
    addTODO( "body" );
  }

  // ######################################## Add Methods ########################################

  /****************************************** addTODO ********************************************/
  protected TableContextMenu addTODO( String name )
  {
    // add a disabled TODO menu item
    var item = new MenuItem( "TODO " + name );
    item.setDisable( true );
    getItems().add( item );
    return this;
  }

  /**************************************** addSeparator *****************************************/
  protected TableContextMenu addSeparator()
  {
    // add a separator menu item
    getItems().add( new SeparatorMenuItem() );
    return this;
  }

  /************************************* addShowAllColumns ***************************************/
  protected TableContextMenu addShowAllColumns()
  {
    // exit without adding menu item if option is omitted for this table-view
    if ( isOmitted( m_view, OptionalMenuItems.SHOW_ALL_COLUMNS ) )
      return this;

    // add a show all columns menu item
    var item = new MenuItem( "Show All Columns" );
    item.setOnAction( event -> HideShow.showAllColumns( m_view ) );
    getItems().add( item );
    return this;
  }

  /*************************************** addShowAllRows ****************************************/
  protected TableContextMenu addShowAllRows()
  {
    // exit without adding menu item if option is omitted for this table-view
    if ( isOmitted( m_view, OptionalMenuItems.SHOW_ALL_ROWS ) )
      return this;

    // add a show all rows menu item
    var item = new MenuItem( "Show All Rows" );
    item.setOnAction( event -> HideShow.showAllRows( m_view ) );
    getItems().add( item );
    return this;
  }

  /*************************************** addHideColumn *****************************************/
  protected TableContextMenu addHideColumn()
  {
    // exit without adding menu item if option is omitted for this table-view
    if ( isOmitted( m_view, OptionalMenuItems.HIDE_COLUMNS ) )
      return this;

    // add a hide column(s) menu item
    var item = new MenuItem( "Hide Column(s)" );
    item.setOnAction( event -> HideShow.hideColumns( m_view, m_mouseCol ) );
    getItems().add( item );
    return this;
  }

  /**************************************** addHideRow *******************************************/
  protected TableContextMenu addHideRow()
  {
    // exit without adding menu item if option is omitted for this table-view
    if ( isOmitted( m_view, OptionalMenuItems.HIDE_ROWS ) )
      return this;

    // add a hide row(s) menu item
    var item = new MenuItem( "Hide Row(s)" );
    item.setOnAction( event -> HideShow.hideRows( m_view, m_mouseRow ) );
    getItems().add( item );
    return this;
  }

  /************************************** addFilterColumn ****************************************/
  protected TableContextMenu addFilterColumn()
  {
    // exit without adding menu item if option is omitted for this table-view
    if ( isOmitted( m_view, OptionalMenuItems.FILTER_COLUMN_CONTAINS_TEXT ) )
      return this;

    // add a filter column menu item
    var hbox = new HBox( 5 );
    var filter = new TextField();
    hbox.getChildren().addAll( new Label( "Filter" ), filter );
    hbox.setAlignment( Pos.CENTER_LEFT );
    var item = new CustomMenuItem( hbox );

    filter.setOnAction( event -> Filter.columnTextContains( m_view, m_mouseCol, filter.getText() ) );
    getItems().add( item );
    return this;
  }

}
