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

package rjc.table.view.cell;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;

import rjc.table.HashSetInt;
import rjc.table.Utils;
import rjc.table.signal.ISignal;
import rjc.table.view.TableView;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/************************ Manages selected cells and areas on table view *************************/
/*************************************************************************************************/

/**
 * Manages a table view's selection as an ordered collection of inclusive rectangles in view
 * coordinates. Areas may overlap, be non-contiguous, or use {@link TableAxis#AFTER} as an open-ended
 * bound. Adding an area does not merge or replace existing areas.
 *
 * <p>Whole-row and whole-column queries combine coverage from all rectangles. Here, visible means
 * unhidden in the relevant axis, including indexes outside the viewport. Opposite-axis hidden
 * cells do not interrupt coverage; a hidden target row or column retains its own selection
 * membership. Empty opposite axes and axes with no visible cells never establish full coverage.</p>
 *
 * <p>Selected-index queries return a set, or {@code null} only when every actual index of that axis
 * is selected, including hidden members. Keyboard classification examines selected visible cells
 * separately, so whole visible-table coverage does not implicitly select additional hidden
 * indexes.</p>
 *
 * <p>Selection operations signal the current number of stored areas through {@link ISignal}, even
 * when visible coverage is unchanged. Unless stated otherwise, they preserve the view's focus and
 * select-cell positions. Use this mutable model on the JavaFX Application Thread.</p>
 */
public class CellSelection implements ISignal
{
  final static private int     INVALID   = TableAxis.INVALID;
  final static private int     FIRSTCELL = TableAxis.FIRSTCELL;
  final static private int     AFTER     = TableAxis.AFTER;

  private TableView            m_view;                         // associated table view
  private ArrayList<Selection> m_selected;                     // selected rectangles in view coordinates

  /***************************************** constructor *****************************************/
  /**
   * Constructs a new cell selection model for the specified table view.
   *
   * @param view the table view this selection model is associated with
   */
  public CellSelection( TableView view )
  {
    // construct selection model
    m_view = view;
    m_selected = new ArrayList<>();
  }

  /******************************************** clear ********************************************/
  /**
   * Removes all selected areas and signals an area count of zero, preserving focus and select-cell
   * positions. Does nothing and emits no signal if the selection is already empty.
   */
  public void clear()
  {
    // remove all selected areas
    if ( m_selected.isEmpty() )
      return;

    m_selected.clear();
    signal( m_selected.size() ); // signal selection model has been cleared
  }

  /****************************************** selectAll ******************************************/
  /**
   * Replaces all areas with one rectangle from {@code (0, 0)} to
   * {@code (TableAxis.AFTER, TableAxis.AFTER)}, including hidden indexes. Signals the new area before
   * setting focus to {@code (0, 0)} and the select cell to the open-ended corner.
   *
   * <p>These are logical positions: they are not adjusted for empty axes or hidden first indexes.</p>
   */
  public void selectAll()
  {
    // select entire table
    m_selected.clear(); // direct clear to avoid clear signal before add signal
    select( FIRSTCELL, FIRSTCELL, AFTER, AFTER );
    m_view.getFocusCell().setPosition( FIRSTCELL, FIRSTCELL );
    m_view.getSelectCell().setPosition( AFTER, AFTER );
  }

  /******************************************* select ********************************************/
  /**
   * Appends a rectangular area defined by two inclusive corners and signals the resulting area
   * count. {@link Selection} normalises corner order and clamps lower bounds to the first body
   * index; upper bounds are not limited to the current table size. Existing areas, focus and
   * select-cell positions are preserved.
   *
   * @param viewColumn1 the column view index of the first corner
   * @param viewRow1    the row view index of the first corner
   * @param viewColumn2 the column view index of the second corner
   * @param viewRow2    the row view index of the second corner
   */
  public void select( int viewColumn1, int viewRow1, int viewColumn2, int viewRow2 )
  {
    // add new selected area to table selected
    Selection newArea = new Selection( viewColumn1, viewRow1, viewColumn2, viewRow2 );
    m_selected.add( newArea );
    signal( m_selected.size() );
  }

  /******************************************* select ********************************************/
  /**
   * Appends the inclusive rectangle between the current focus and select-cell positions by
   * delegating to {@link #select(int, int, int, int)}. Does not clear existing areas or change
   * either position.
   */
  public void select()
  {
    // select new area based on focus & select cell positions
    ViewPosition focus = m_view.getFocusCell();
    ViewPosition select = m_view.getSelectCell();
    select( focus.getColumn(), focus.getRow(), select.getColumn(), select.getRow() );
  }

  /**************************************** selectColumns ****************************************/
  /**
   * Appends areas for the supplied columns, grouping consecutive indexes into one area. Each area
   * spans rows from zero to {@link TableAxis#AFTER}. Existing areas and focus/select-cell positions
   * are preserved. Signals the final area count even if the supplied set is empty.
   *
   * @param viewColumnsToSelect a non-null set of body column view indexes to add
   */
  public void selectColumns( HashSetInt viewColumnsToSelect )
  {
    // select specified columns by creating areas for contiguous ranges
    var sortedColumns = viewColumnsToSelect.toSortedArray();
    int startColumn = INVALID;
    int endColumn = INVALID;

    // iterate through sorted columns to identify contiguous ranges
    for ( int columnIndex : sortedColumns )
    {
      if ( startColumn == INVALID )
      {
        // start new range
        startColumn = columnIndex;
        endColumn = columnIndex;
      }
      else if ( columnIndex == endColumn + 1 )
        // extend current range
        endColumn = columnIndex;
      else
      {
        // save current range and start new range
        Selection newArea = new Selection( startColumn, FIRSTCELL, endColumn, AFTER );
        m_selected.add( newArea );
        startColumn = columnIndex;
        endColumn = columnIndex;
      }
    }

    // add final range if one exists
    if ( startColumn != INVALID )
    {
      Selection newArea = new Selection( startColumn, FIRSTCELL, endColumn, AFTER );
      m_selected.add( newArea );
    }
    signal( m_selected.size() );
  }

  /***************************************** selectRows ******************************************/
  /**
   * Appends areas for the supplied rows, grouping consecutive indexes into one area. Each area
   * spans columns from zero to {@link TableAxis#AFTER}. Existing areas and focus/select-cell
   * positions are preserved. Signals the final area count even if the supplied set is empty.
   *
   * @param viewRowsToSelect a non-null set of body row view indexes to add
   */
  public void selectRows( HashSetInt viewRowsToSelect )
  {
    // select specified rows by creating areas for contiguous ranges
    var sortedRows = viewRowsToSelect.toSortedArray();
    int startRow = INVALID;
    int endRow = INVALID;

    // iterate through sorted rows to identify contiguous ranges
    for ( int rowIndex : sortedRows )
    {
      if ( startRow == INVALID )
      {
        // start new range
        startRow = rowIndex;
        endRow = rowIndex;
      }
      else if ( rowIndex == endRow + 1 )
        // extend current range
        endRow = rowIndex;
      else
      {
        // save current range and start new range
        Selection newArea = new Selection( FIRSTCELL, startRow, AFTER, endRow );
        m_selected.add( newArea );
        startRow = rowIndex;
        endRow = rowIndex;
      }
    }

    // add final range if one exists
    if ( startRow != INVALID )
    {
      Selection newArea = new Selection( FIRSTCELL, startRow, AFTER, endRow );
      m_selected.add( newArea );
    }
    signal( m_selected.size() );
  }

  /******************************************* update ********************************************/
  /**
   * Replaces the bounds of the last area using the current focus and select-cell positions, then
   * signals the area count. If no area exists, first creates one through {@link #select()}.
   *
   * <p>If the select cell is at or beyond an axis's body count, the area's starting coordinate on
   * that axis uses zero instead of the focus coordinate. This establishes whole-row/column bounds
   * without moving the actual focus or select cell. Earlier areas remain unchanged.</p>
   */
  public void update()
  {
    // update last selected area to match current focus and select cell positions
    ViewPosition focus = m_view.getFocusCell();
    ViewPosition select = m_view.getSelectCell();
    if ( m_selected.isEmpty() )
      select();
    Selection lastSelectedArea = m_selected.get( m_selected.size() - 1 );

    // if selecting columns or rows, then start selecting from first cell
    int anchorColumn = select.isColumnAfter() ? FIRSTCELL : focus.getColumn();
    int anchorRow = select.isRowAfter() ? FIRSTCELL : focus.getRow();
    lastSelectedArea.set( anchorColumn, anchorRow, select.getColumn(), select.getRow() );
    signal( m_selected.size() );
  }

  /*************************************** isCellSelected ****************************************/
  /**
   * Tests whether the supplied position lies within any stored rectangle. This is a geometric
   * test: it does not check axis visibility or current table bounds, so an open-ended area may
   * also contain positions beyond the current body count.
   *
   * @param viewColumn the column view index to check
   * @param viewRow    the row view index to check
   * @return true if a stored area contains the position; false otherwise
   */
  public boolean isCellSelected( int viewColumn, int viewRow )
  {
    // return true if specified cell is in a selected area
    for ( var area : m_selected )
      if ( area.isCellSelected( viewColumn, viewRow ) )
        return true;

    return false;
  }

  /************************************** isColumnSelected ***************************************/
  /**
   * Tests whether the union of selected areas covers every unhidden row in the specified column.
   * The target column may itself be hidden. Returns false for an invalid column index, an empty
   * or all-hidden row axis, or any uncovered visible row.
   *
   * @param viewColumn the column view index to check
   * @return true if the valid column has complete visible-row coverage; false otherwise
   */
  public boolean isColumnSelected( int viewColumn )
  {
    return isIndexSelected( true, viewColumn );
  }

  /**************************************** isRowSelected ****************************************/
  /**
   * Tests whether the union of selected areas covers every unhidden column in the specified row.
   * The target row may itself be hidden. Returns false for an invalid row index, an empty or
   * all-hidden column axis, or any uncovered visible column.
   *
   * @param viewRow the row view index to check
   * @return true if the valid row has complete visible-column coverage; false otherwise
   */
  public boolean isRowSelected( int viewRow )
  {
    return isIndexSelected( false, viewRow );
  }

  /************************************* hasColumnSelection **************************************/
  /**
   * Tests whether the supplied index lies within any stored area's column bounds, regardless of
   * full-column coverage. Does not validate the index, axis visibility, or the area's row bounds.
   *
   * @param viewColumn the column view index to check
   * @return true if an area's column range contains the index; false otherwise
   */
  public boolean hasColumnSelection( int viewColumn )
  {
    // return true if specified column has any selection
    for ( var area : m_selected )
      if ( viewColumn >= area.c1 && viewColumn <= area.c2 )
        return true;

    return false;
  }

  /*************************************** hasRowSelection ***************************************/
  /**
   * Tests whether the supplied index lies within any stored area's row bounds, regardless of
   * full-row coverage. Does not validate the index, axis visibility, or the area's column bounds.
   *
   * @param viewRow the row view index to check
   * @return true if an area's row range contains the index; false otherwise
   */
  public boolean hasRowSelection( int viewRow )
  {
    // return true if specified row has any selection
    for ( var area : m_selected )
      if ( viewRow >= area.r1 && viewRow <= area.r2 )
        return true;

    return false;
  }

  /************************************* getResizableColumns **************************************/
  /**
   * Filters the selection from {@link #getSelectedColumns()} using the view's column-resizability
   * rules. Returns the all-indexes sentinel {@code null} only if every column is selected and resizable.
   *
   * @return a new filtered set, an empty set if no eligible columns exist, or {@code null} if every
   *         column is selected and resizable
   */
  public HashSetInt getResizableColumns()
  {
    return getResizableIndexes( true );
  }

  /************************************* getSelectedColumns **************************************/
  /**
   * Returns valid column view indexes whose combined selection areas cover every visible row,
   * including hidden columns that satisfy that coverage. Hidden rows do not interrupt coverage.
   *
   * <p>Returns {@code null} only when every actual column index is included. Selecting all visible
   * columns alone does not add unselected hidden columns. An empty column axis or a row axis with
   * no visible rows produces an empty set.</p>
   *
   * @return a new set of fully selected column indexes, or {@code null} for all actual columns
   */
  public HashSetInt getSelectedColumns()
  {
    return getSelectedIndexes( true );
  }

  /************************************** getResizableRows ***************************************/
  /**
   * Filters the selection from {@link #getSelectedRows()} using the view's row-resizability rules.
   * Returns the all-indexes sentinel {@code null} only if every row is selected and resizable.
   *
   * @return a new filtered set, an empty set if no eligible rows exist, or {@code null} if every
   *         row is selected and resizable
   */
  public HashSetInt getResizableRows()
  {
    return getResizableIndexes( false );
  }

  /*************************************** getSelectedRows ***************************************/
  /**
   * Returns valid row view indexes whose combined selection areas cover every visible column,
   * including hidden rows that satisfy that coverage. Hidden columns do not interrupt coverage.
   *
   * <p>Returns {@code null} only when every actual row index is included. Selecting all visible rows
   * alone does not add unselected hidden rows. An empty row axis or a column axis with no visible
   * columns produces an empty set.</p>
   *
   * @return a new set of fully selected row indexes, or {@code null} for all actual rows
   */
  public HashSetInt getSelectedRows()
  {
    return getSelectedIndexes( false );
  }

  /********************************** areAllVisibleRowsSelected **********************************/
  /**
   * Tests whether every visible row has full visible-column coverage. Hidden indexes outside the
   * selected areas do not affect this visible-table test or become selected as a result.
   *
   * @return true if every visible cell is selected and both axes have visible indexes; false otherwise
   */
  public boolean areAllVisibleRowsSelected()
  {
    var axis = m_view.getRowsAxis();
    return coversVisibleRange( axis, getSelectedRanges( false ), 0, axis.getCount() - 1 );
  }

  /******************************** areAllVisibleColumnsSelected *********************************/
  /**
   * Tests whether every visible column has full visible-row coverage. Hidden indexes outside the
   * selected areas do not affect this visible-table test or become selected as a result.
   *
   * @return true if every visible cell is selected and both axes have visible indexes; false otherwise
   */
  public boolean areAllVisibleColumnsSelected()
  {
    var axis = m_view.getColumnsAxis();
    return coversVisibleRange( axis, getSelectedRanges( true ), 0, axis.getCount() - 1 );
  }

  /************************************* areOnlyRowsSelected *************************************/
  /**
   * Tests whether the selected visible cells consist entirely of whole rows. Requires at least
   * one selected visible cell; any additional partial visible row makes the result false.
   * Overlapping or redundant rectangles and selection confined to hidden cells do not alter the
   * visible classification.
   *
   * <p>Whole visible-table coverage makes both this method and {@link #areOnlyColumnsSelected()}
   * return true. Callers choosing an exclusive menu region must handle that case separately.</p>
   *
   * @return true for a nonempty visible selection consisting of whole rows, including the whole table
   */
  public boolean areOnlyRowsSelected()
  {
    return areOnlyIndexesSelected( false );
  }

  /*********************************** areOnlyColumnsSelected ************************************/
  /**
   * Tests whether the selected visible cells consist entirely of whole columns. Requires at least
   * one selected visible cell; any additional partial visible column makes the result false.
   * Overlapping or redundant rectangles and selection confined to hidden cells do not alter the
   * visible classification.
   *
   * <p>Whole visible-table coverage makes both this method and {@link #areOnlyRowsSelected()} return
   * true. Callers choosing an exclusive menu region must handle that case separately.</p>
   *
   * @return true for a nonempty visible selection consisting of whole columns, including the whole table
   */
  public boolean areOnlyColumnsSelected()
  {
    return areOnlyIndexesSelected( true );
  }

  /*************************************** isIndexSelected ***************************************/
  /**
   * Collects and sorts opposite-axis coverage from rectangles containing an index, then tests
   * their union against all visible indexes on that opposite axis. The target index may be hidden.
   *
   * @param columns true for a target column; false for a target row
   * @param index the body view index to test
   * @return true for a valid index with complete nonempty visible opposite-axis coverage
   */
  private boolean isIndexSelected( boolean columns, int index )
  {
    var axis = columns ? m_view.getColumnsAxis() : m_view.getRowsAxis();
    var opposite = columns ? m_view.getRowsAxis() : m_view.getColumnsAxis();
    if ( index < 0 || index >= axis.getCount() )
      return false;

    // merge coverage from every rectangle containing this index
    var ranges = new ArrayList<int[]>();
    for ( var area : m_selected )
      if ( columns ? index >= area.c1 && index <= area.c2 : index >= area.r1 && index <= area.r2 )
        ranges.add( columns ? new int[] { area.r1, area.r2 } : new int[] { area.c1, area.c2 } );
    ranges.sort( Comparator.comparingInt( range -> range[0] ) );
    return coversVisibleRange( opposite, ranges, 0, opposite.getCount() - 1 );
  }

  /************************************** getSelectedRanges **************************************/
  /**
   * Finds fully selected indexes as sorted, disjoint inclusive ranges, merging adjacent results.
   * Evaluates coverage between rectangle edges rather than visiting every target index or cell.
   * Ranges are bounded by the current axis count and retain selected hidden target indexes.
   *
   * @param columns true to find column ranges; false to find row ranges
   * @return a new list of {@code [start, end]} pairs; empty if the target axis is empty or the
   *         opposite axis has no visible indexes
   */
  private ArrayList<int[]> getSelectedRanges( boolean columns )
  {
    var axis = columns ? m_view.getColumnsAxis() : m_view.getRowsAxis();
    var opposite = columns ? m_view.getRowsAxis() : m_view.getColumnsAxis();
    var ranges = new ArrayList<int[]>();
    if ( axis.getCount() == 0 || opposite.getFirstVisible() < 0 )
      return ranges;
    var edges = new TreeSet<Integer>();
    for ( var area : m_selected )
    {
      int start = Math.max( 0, columns ? area.c1 : area.r1 );
      int end = Math.min( axis.getCount() - 1, columns ? area.c2 : area.r2 );
      if ( start <= end )
      {
        edges.add( start );
        edges.add( end + 1 );
      }
    }

    // coverage can change only at rectangle edges, regardless of the number of table cells
    int previous = INVALID;
    for ( int edge : edges )
    {
      if ( previous != INVALID && isIndexSelected( columns, previous ) )
      {
        if ( !ranges.isEmpty() && ranges.getLast()[1] == previous - 1 )
          ranges.getLast()[1] = edge - 1;
        else
          ranges.add( new int[] { previous, edge - 1 } );
      }
      previous = edge;
    }
    return ranges;
  }

  /************************************** getResizableIndexes ************************************/
  private HashSetInt getResizableIndexes( boolean columns )
  {
    var selected = columns ? getSelectedColumns() : getSelectedRows();
    if ( selected != null )
    {
      selected.removeIf( index -> !( columns ? m_view.isColumnResizable( index ) : m_view.isRowResizable( index ) ) );
      return selected;
    }

    // retain the all-indexes sentinel until a restriction requires an explicit set
    var axis = columns ? m_view.getColumnsAxis() : m_view.getRowsAxis();
    for ( int index = 0; index < axis.getCount(); index++ )
      if ( columns ? m_view.isColumnResizable( index ) : m_view.isRowResizable( index ) )
      {
        if ( selected != null )
          selected.add( index );
      }
      else if ( selected == null )
      {
        selected = new HashSetInt();
        for ( int previous = 0; previous < index; previous++ )
          selected.add( previous );
      }
    return selected;
  }

  /************************************** getSelectedIndexes *************************************/
  /**
   * Expands fully selected ranges into an explicit set, retaining the {@code null} convention only
   * when the ranges cover every actual index of a nonempty axis, including hidden members.
   *
   * @param columns true to enumerate columns; false to enumerate rows
   * @return a new set of selected valid view indexes, possibly empty, or {@code null} for all indexes
   */
  private HashSetInt getSelectedIndexes( boolean columns )
  {
    var axis = columns ? m_view.getColumnsAxis() : m_view.getRowsAxis();
    var ranges = getSelectedRanges( columns );
    if ( ranges.size() == 1 && ranges.getFirst()[0] == 0 && ranges.getFirst()[1] == axis.getCount() - 1 )
      return null; // all actual indexes, including hidden members, are selected

    var selected = new HashSetInt();
    for ( var range : ranges )
      for ( int index = range[0]; index <= range[1]; index++ )
        selected.add( index );
    return selected;
  }

  /************************************ areOnlyIndexesSelected ***********************************/
  /**
   * Checks that every rectangle contributing visible cells lies within fully selected target-axis
   * ranges. Areas containing only hidden cells are ignored. Whole visible-table coverage qualifies
   * for either orientation.
   *
   * @param columns true to classify whole-column selection; false to classify whole-row selection
   * @return true if at least one visible cell is selected and no partial visible target index remains
   */
  private boolean areOnlyIndexesSelected( boolean columns )
  {
    var axis = columns ? m_view.getColumnsAxis() : m_view.getRowsAxis();
    var opposite = columns ? m_view.getRowsAxis() : m_view.getColumnsAxis();
    if ( axis.getFirstVisible() < 0 || opposite.getFirstVisible() < 0 )
      return false;
    var ranges = getSelectedRanges( columns );
    boolean anyVisible = false;
    for ( var area : m_selected )
    {
      int start = Math.max( 0, columns ? area.c1 : area.r1 );
      int end = Math.min( axis.getCount() - 1, columns ? area.c2 : area.r2 );
      if ( hasVisibleIndex( axis, start, end )
          && hasVisibleIndex( opposite, columns ? area.r1 : area.c1, columns ? area.r2 : area.c2 ) )
      {
        anyVisible = true;
        if ( !coversVisibleRange( axis, ranges, start, end ) )
          return false; // an additional partial row/column makes the complete selection mixed
      }
    }
    return anyVisible;
  }

  /************************************* coversVisibleRange **************************************/
  /**
   * Tests whether inclusive coverage intervals contain every unhidden index in a query range.
   * Intervals must be sorted by start and may overlap. Hidden gaps do not interrupt coverage, and
   * the supplied interval list is not modified.
   *
   * @param axis the axis defining valid indexes and visibility
   * @param ranges covered {@code [start, end]} intervals in ascending start order
   * @param start the inclusive query start
   * @param end the inclusive query end
   * @return true if the query contains at least one visible index and every such index is covered
   */
  private boolean coversVisibleRange( TableAxis axis, ArrayList<int[]> ranges, int start, int end )
  {
    if ( !hasVisibleIndex( axis, start, end ) )
      return false; // empty and all-hidden axes never qualify through vacuous truth

    int next = start;
    for ( var range : ranges )
    {
      if ( range[1] < next )
        continue;
      if ( hasVisibleIndex( axis, next, Math.min( end, range[0] - 1 ) ) )
        return false;
      next = Math.max( next, Math.min( end, range[1] ) + 1 );
      if ( next > end )
        return true;
    }
    return !hasVisibleIndex( axis, next, end );
  }

  /*************************************** hasVisibleIndex ***************************************/
  /**
   * Checks for an unhidden index in an inclusive range after limiting it to the current body
   * bounds. Verifies the navigation result lies in that range because axis navigation may fall
   * back to an earlier index when no later visible index exists.
   *
   * @param axis the axis to inspect
   * @param start the inclusive range start
   * @param end the inclusive range end
   * @return true if a valid unhidden index exists within the range; false for an empty range
   */
  private boolean hasVisibleIndex( TableAxis axis, int start, int end )
  {
    start = Math.max( 0, start );
    end = Math.min( axis.getCount() - 1, end );
    if ( start > end )
      return false;
    int visible = axis.getNextVisible( start - 1 );
    return visible >= start && visible <= end;
  }

  /****************************************** getAreas *******************************************/
  /**
   * Copies the stored areas in order as inclusive
   * {@code [columnStart, rowStart, columnEnd, rowEnd]} arrays. End coordinates are clamped to current
   * body bounds; start coordinates, overlaps and area order are retained. An empty axis or an area
   * beyond the current table can therefore yield an end before its start.
   *
   * <p>The list and arrays are independent snapshots. Modifying them does not alter selection.
   * Used by the canvas overlay to draw selection highlighting.</p>
   *
   * @return a new list containing one four-element bounds array per stored area
   */
  public ArrayList<int[]> getAreas()
  {
    // return list of selected areas - used by CanvasOverlay to draw the highlighting
    int maxColumnIndex = m_view.getColumnsAxis().getCount() - 1;
    int maxRowIndex = m_view.getRowsAxis().getCount() - 1;
    var selectedAreas = new ArrayList<int[]>();

    // construct the list with clamped coordinates
    for ( Selection selectedArea : m_selected )
    {
      int[] areaBounds = { selectedArea.c1, selectedArea.r1, Math.min( selectedArea.c2, maxColumnIndex ),
          Math.min( selectedArea.r2, maxRowIndex ) };
      selectedAreas.add( areaBounds );
    }

    return selectedAreas;
  }

  /****************************************** toString *******************************************/
  /**
   * Returns a diagnostic description of this selection model and its stored rectangles.
   *
   * @return the model identity and selected-area list
   */
  @Override
  public String toString()
  {
    // convert to string
    return Utils.name( this ) + "[selected=" + m_selected + "]";
  }

}
