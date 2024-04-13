/**************************************************************************
 *  Copyright (C) 2024 by Richard Crook                                   *
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

package rjc.table.view.events;

import javafx.event.EventHandler;
import javafx.scene.input.KeyEvent;
import rjc.table.Utils;
import rjc.table.view.TableView;
import rjc.table.view.action.Zoom;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/************************ Handles keyboard pressed events for table-view *************************/
/*************************************************************************************************/

public class KeyPressed implements EventHandler<KeyEvent>
{
  private TableView m_view;
  private boolean   m_shift;
  private boolean   m_ctrl;
  private boolean   m_alt;

  /******************************************* handle ********************************************/
  @Override
  public void handle( KeyEvent event )
  {
    // user has pressed a keyboard key
    event.consume();
    m_view = (TableView) event.getSource();
    m_shift = event.isShiftDown();
    m_ctrl = event.isControlDown();
    m_alt = event.isAltDown();
    m_view.getStatus().clear();

    // handle arrow, page, home, end, insert, delete, F2 & F5 keys
    if ( !m_alt )
      switch ( event.getCode() )
      {
        case RIGHT: // right -> arrow key
        case KP_RIGHT:
          moveRight();
          return;

        case LEFT: // left <- arrow key
        case KP_LEFT:
          moveLeft();
          return;

        case DOWN: // down arrow key
        case KP_DOWN:
          moveDown();
          return;

        case UP: // up arrow key
        case KP_UP:
          moveUp();
          return;

        case PAGE_DOWN: // page down key
          pageDown();
          return;

        case PAGE_UP: // page up key
          pageUp();
          return;

        case HOME: // home key - navigate to left-most visible column
          moveHome();
          return;

        case END: // end key - navigate to right-most visible column
          moveEnd();
          return;

        case DELETE: // delete key - delete selected cells content
          // Content.delete( m_view );
          return;

        case INSERT: // insert key - insert row or column
          // Content.insert( m_view );
          return;

        case F2: // F2 key - open cell editor with current focus cell contents
          openEditor();
          return;

        case F5: // F5 key - redraw table
          m_view.redraw();
          return;

        default:
          break;
      }

    // handle control keys
    if ( m_ctrl && !m_alt )
      switch ( event.getCode() )
      {
        case A: // select whole table (Ctrl-A)
          m_view.getSelection().selectAll();
          return;

        case X: // cut cells contents (Ctrl-X)
          // Content.cut( m_view );
          return;

        case C: // copy cells contents (Ctrl-C)
          // Content.copy( m_view );
          return;

        case V: // paste cells contents (Ctrl-V)
          // Content.paste( m_view );
          return;

        case D: // fill-down cells contents (Ctrl-D)
          // Content.fillDown( m_view );
          return;

        case Z: // undo command (Ctrl-Z)
          m_view.getUndoStack().undo();
          return;

        case Y: // redo command (Ctrl-Y)
          m_view.getUndoStack().redo();
          return;

        case MINUS: // zoom out (Ctrl-minus)
        case SUBTRACT:
          Zoom.setZoom( m_view, m_view.getZoom().get() / Math.pow( 2.0, 1.0 / 16.0 ) );
          return;

        case EQUALS: // zoom in (Ctrl-plus)
        case ADD:
          Zoom.setZoom( m_view, m_view.getZoom().get() * Math.pow( 2.0, 1.0 / 16.0 ) );
          return;

        case DIGIT0: // reset zoom 1:1 (Ctrl-0)
        case NUMPAD0:
          Zoom.setZoom( m_view, 1.0 );
          return;

        default:
          break;
      }

  }

  /************************************* checkSelectToFocus **************************************/
  private void checkSelectToFocus()
  {
    // does new selection need to be started
    if ( !m_shift )
    {
      m_view.getSelectCell().setPosition( m_view.getFocusCell() );
      m_view.getSelection().clear();
    }
  }

  /************************************* checkFocusToSelect **************************************/
  private void checkFocusToSelect()
  {
    // does focus need to be moved to select
    if ( !m_shift )
      m_view.getFocusCell().setPosition( m_view.getSelectCell() );
  }

  /****************************************** moveRight ******************************************/
  private void moveRight()
  {
    // move selected and focus cell position right
    checkSelectToFocus();
    if ( m_ctrl )
      m_view.getSelectCell().moveRightEdge();
    else
      m_view.getSelectCell().moveRight();
    checkFocusToSelect();
  }

  /****************************************** moveLeft *******************************************/
  private void moveLeft()
  {
    // move selected and focus cell position left
    checkSelectToFocus();
    if ( m_ctrl )
      m_view.getSelectCell().moveLeftEdge();
    else
      m_view.getSelectCell().moveLeft();
    checkFocusToSelect();
  }

  /****************************************** moveDown *******************************************/
  private void moveDown()
  {
    // move selected and focus cell position down
    checkSelectToFocus();
    if ( m_ctrl )
      m_view.getSelectCell().moveBottom();
    else
      m_view.getSelectCell().moveDown();
    checkFocusToSelect();
  }

  /******************************************* moveUp ********************************************/
  private void moveUp()
  {
    // move selected and focus cell position up
    checkSelectToFocus();
    if ( m_ctrl )
      m_view.getSelectCell().moveTop();
    else
      m_view.getSelectCell().moveUp();
    checkFocusToSelect();
  }

  /****************************************** pageDown *******************************************/
  private void pageDown()
  {
    // scroll table down one page
    checkSelectToFocus();
    var scrollbar = m_view.getVerticalScrollBar();
    var canvas = m_view.getCanvas();
    int headerHeight = m_view.getHeaderHeight();
    int value = (int) scrollbar.getValue();

    if ( scrollbar.isVisible() && value < scrollbar.getMax() )
    {
      // bottom of table not visible
      int newValue = value + (int) canvas.getHeight() - headerHeight;
      int topIndex = m_view.getRowIndex( newValue );

      // make sure scroll down at least one row
      if ( topIndex == m_view.getRowIndex( headerHeight ) )
        topIndex = canvas.getRowsAxis().getNextVisible( topIndex );
      newValue = m_view.getRowStartY( topIndex );

      // determine new position for select cell
      m_view.getSelectCell().moveToVisible();
      int selectY = m_view.getRowStartY( m_view.getSelectCell().getRow() );
      selectY = selectY - value + newValue;
      int newRow = m_view.getRowIndex( selectY );
      if ( newRow < TableAxis.AFTER )
        m_view.getSelectCell().setRow( newRow );
      else
        m_view.getSelectCell().setRow( canvas.getRowsAxis().getLastVisible() );

      checkFocusToSelect();
      scrollbar.laterScrollToValue( newValue );
    }
    else
    {
      // bottom of table already visible so move to last row
      m_view.getSelectCell().moveBottom();
      checkFocusToSelect();
    }
  }

  /******************************************* pageUp ********************************************/
  private void pageUp()
  {
    // scroll table up one page
    checkSelectToFocus();
    var scrollbar = m_view.getVerticalScrollBar();
    var canvas = m_view.getCanvas();
    int headerHeight = m_view.getHeaderHeight();
    int value = (int) scrollbar.getValue();

    if ( scrollbar.isVisible() && value > 0 )
    {
      // top of table not visible
      int newValue = value - (int) canvas.getHeight() + headerHeight;
      int topIndex = m_view.getRowIndex( newValue );
      newValue = m_view.getRowStartY( canvas.getRowsAxis().getNextVisible( topIndex ) );
      if ( newValue < scrollbar.getMin() )
        newValue = (int) scrollbar.getMin();

      // determine new position for select cell
      m_view.getSelectCell().moveToVisible();
      int selectY = m_view.getRowStartY( m_view.getSelectCell().getRow() );
      scrollbar.setValue( newValue );
      int newRow = m_view.getRowIndex( selectY );
      scrollbar.setValue( value );
      if ( newRow > TableAxis.HEADER )
        m_view.getSelectCell().setRow( newRow );
      else
        m_view.getSelectCell().setRow( canvas.getRowsAxis().getFirstVisible() );

      checkFocusToSelect();
      scrollbar.laterScrollToValue( newValue );
    }
    else
    {
      // top of table already visible so move to first row
      m_view.getSelectCell().moveTop();
      checkFocusToSelect();
    }
  }

  /****************************************** moveHome *******************************************/
  private void moveHome()
  {
    // move selected and focus cell position home (left edge)
    checkSelectToFocus();
    m_view.getSelectCell().moveLeftEdge();

    if ( m_ctrl )
      m_view.getSelectCell().moveTop();
    checkFocusToSelect();
  }

  /****************************************** moveEnd ********************************************/
  private void moveEnd()
  {
    // move selected and focus cell position end (right edge)
    checkSelectToFocus();
    m_view.getSelectCell().moveRightEdge();

    if ( m_ctrl )
      m_view.getSelectCell().moveBottom();
    checkFocusToSelect();
  }

  /***************************************** openEditor ******************************************/
  private void openEditor()
  {
    // open cell editor with current focus cell contents
    Utils.trace( "TODO open editor" );
  }

}