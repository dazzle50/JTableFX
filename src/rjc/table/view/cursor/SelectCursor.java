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

package rjc.table.view.cursor;

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import rjc.table.view.TableScrollBar.Animation;

/*************************************************************************************************/
/******************* Mouse cursor when selecting table view cells/columns/rows *******************/
/*************************************************************************************************/

public class SelectCursor extends AbstractCursor
{

  /**************************************** constructor ******************************************/
  public SelectCursor( String imageFile, int xHotspot, int yHotstop )
  {
    super( imageFile, xHotspot, yHotstop );
  }

  /*************************************** handleReleased ****************************************/
  @Override
  public void handleReleased( MouseEvent event )
  {
    // finishing selecting, so update cursor and stop any animations
    m_view.getMouseCell().setXY( m_x, m_y, true );
    m_view.getHorizontalScrollBar().stopAnimationStartEnd();
    m_view.getVerticalScrollBar().stopAnimationStartEnd();
  }

  /**************************************** handleDragged ****************************************/
  @Override
  public void handleDragged( MouseEvent event )
  {
    // selecting cells by dragging mouse whilst primary button held down
    extractDetails( event );
    if ( m_button != MouseButton.PRIMARY )
      return;

    // check if table scrolling is wanted
    checkScrollingX();
    checkScrollingY();

    // update mouse cell position
    m_mouseCell.setXY( m_x, m_y, false );
  }

  /************************************* checkSelectPosition *************************************/
  @Override
  public void checkSelectPosition()
  {
    // update select cell position only if cursor is selecting
    int column = checkColumnPosition();
    int row = checkRowPosition();
    m_view.getSelectCell().setPosition( column, row );
  }

  /************************************* checkColumnPosition *************************************/
  private int checkColumnPosition()
  {
    // if mouse is beyond the table, limit to last visible column
    var axis = m_view.getColumnsAxis();
    int column = Math.min( axis.getLastVisible(), m_mouseCell.getColumn() );

    // if selecting rows ignore mouse column
    column = this == Cursors.ROWS_SELECT ? m_selectCell.getColumn() : column;

    // if animating to start or end, ensure selection edge is visible on view
    var animation = m_view.getHorizontalScrollBar().getAnimation();
    if ( animation == Animation.TO_START )
    {
      column = m_view.getColumnIndex( m_view.getHeaderWidth() );
      column = axis.getNextVisible( column );
    }
    if ( animation == Animation.TO_END )
    {
      column = m_view.getColumnIndex( (int) m_view.getCanvas().getWidth() );
      column = axis.getPreviousVisible( column );
    }

    return column;
  }

  /************************************** checkRowPosition ***************************************/
  private int checkRowPosition()
  {
    // if mouse is beyond the table, limit to last visible row
    var rowAxis = m_view.getRowsAxis();
    int row = Math.min( rowAxis.getLastVisible(), m_mouseCell.getRow() );

    // if selecting columns ignore mouse row
    row = this == Cursors.COLUMNS_SELECT ? m_selectCell.getRow() : row;

    var animation = m_view.getVerticalScrollBar().getAnimation();
    if ( animation == Animation.TO_START )
    {
      row = m_view.getRowIndex( m_view.getHeaderHeight() );
      row = rowAxis.getNextVisible( row );
    }
    if ( animation == Animation.TO_END )
    {
      row = m_view.getRowIndex( (int) m_view.getCanvas().getHeight() );
      row = rowAxis.getPreviousVisible( row );
    }

    return row;
  }
}
