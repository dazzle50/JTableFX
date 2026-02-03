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

import rjc.table.Utils;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/********************* Represents one selected rectangle of table-view cells *********************/
/*************************************************************************************************/

/**
 * Represents a single rectangular selection area within a table view. The selection is defined
 * by two corner positions, with coordinates automatically normalised so that c1 ≤ c2 and r1 ≤ r2.
 * Coordinates are also clamped to be at least FIRSTCELL to ensure valid table positions.
 * <p>
 * This class uses package-accessible fields for efficient access by CellSelection and related
 * classes within the cell package.
 */
public class Selection
{
  final static private int FIRSTCELL = TableAxis.FIRSTCELL;

  public int               c1;                             // smallest column index in selection
  public int               r1;                             // smallest row index in selection
  public int               c2;                             // largest column index in selection
  public int               r2;                             // largest row index in selection

  /***************************************** constructor *****************************************/
  /**
   * Constructs a new selection rectangle defined by two corner positions. The coordinates are
   * automatically normalised so that c1 ≤ c2 and r1 ≤ r2, and clamped to be at least FIRSTCELL.
   *
   * @param viewColumn1 the column index of the first corner
   * @param viewRow1    the row index of the first corner
   * @param viewColumn2 the column index of the second corner
   * @param viewRow2    the row index of the second corner
   */
  public Selection( int viewColumn1, int viewRow1, int viewColumn2, int viewRow2 )
  {
    // initialise selection area with normalised coordinates
    set( viewColumn1, viewRow1, viewColumn2, viewRow2 );
  }

  /********************************************* set *********************************************/
  /**
   * Updates this selection rectangle with new corner positions. The coordinates are automatically
   * normalised so that c1 ≤ c2 and r1 ≤ r2, and clamped to be at least FIRSTCELL to ensure
   * valid table positions.
   *
   * @param viewColumn1 the column index of the first corner
   * @param viewRow1    the row index of the first corner
   * @param viewColumn2 the column index of the second corner
   * @param viewRow2    the row index of the second corner
   */
  public void set( int viewColumn1, int viewRow1, int viewColumn2, int viewRow2 )
  {
    // normalise column indices and clamp to minimum valid value
    c1 = Math.min( viewColumn1, viewColumn2 );
    c1 = c1 < FIRSTCELL ? FIRSTCELL : c1;
    c2 = Math.max( viewColumn1, viewColumn2 );

    // normalise row indices and clamp to minimum valid value
    r1 = Math.min( viewRow1, viewRow2 );
    r1 = r1 < FIRSTCELL ? FIRSTCELL : r1;
    r2 = Math.max( viewRow1, viewRow2 );
  }

  /*************************************** isCellSelected ****************************************/
  /**
   * Determines whether the specified cell position is contained within this selection rectangle.
   * A cell is considered selected if its column index is between c1 and c2 (inclusive) and its
   * row index is between r1 and r2 (inclusive).
   *
   * @param viewColumn the column index to check
   * @param viewRow    the row index to check
   * @return true if the specified position is within this selection, false otherwise
   */
  public boolean isCellSelected( int viewColumn, int viewRow )
  {
    // return true if specified position is within selection bounds
    return viewColumn >= c1 && viewColumn <= c2 && viewRow >= r1 && viewRow <= r2;
  }

  /****************************************** toString *******************************************/
  /**
   * Returns a string representation of this selection showing its bounding coordinates.
   *
   * @return a string in the format "Selection[c1=x r1=y c2=z r2=w]"
   */
  @Override
  public String toString()
  {
    // convert to string for debugging showing all corner coordinates
    return Utils.name( this ) + "[c1=" + c1 + " r1=" + r1 + " c2=" + c2 + " r2=" + r2 + "]";
  }
}