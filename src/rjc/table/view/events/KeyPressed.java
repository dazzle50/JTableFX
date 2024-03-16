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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import rjc.table.Utils;
import rjc.table.view.TableView;

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

    // handle the key press
    if ( handleFocusSelectMovement( event.getCode() ) )
      return;
    if ( handleZoom( event.getCode() ) )
      return;

    // handle control keys
    if ( m_ctrl && !m_alt )
      switch ( event.getCode() )
      {
        case Z: // undo command (Ctrl-Z)
          // TODO m_view.getUndoStack().undo();
          return;

        case Y: // redo command (Ctrl-Y)
          // TODO m_view.getUndoStack().redo();
          return;

        default:
          break;
      }

  }

  /***************************************** handleZoom ******************************************/
  private boolean handleZoom( KeyCode code )
  {
    // handle movement only if ALT not pressed
    if ( m_alt || !m_ctrl )
      return false;

    // handle control keys
    switch ( code )
    {
      case MINUS: // zoom out (Ctrl-minus)
      case SUBTRACT:
        // TODO Zoom.setZoom( m_view, m_view.getZoom().get() / Math.pow( 2.0, 0.0625 ) );
        return true;

      case EQUALS: // zoom in (Ctrl-plus)
      case ADD:
        // TODO Zoom.setZoom( m_view, m_view.getZoom().get() * Math.pow( 2.0, 0.0625 ) );
        return true;

      case DIGIT0: // reset zoom 1:1 (Ctrl-0)
      case NUMPAD0:
        // TODO Zoom.setZoom( m_view, 1.0 );
        return true;

      default:
        return false;
    }
  }

  /********************************** handleFocusSelectMovement **********************************/
  private boolean handleFocusSelectMovement( KeyCode code )
  {
    // handle movement only if ALT not pressed
    if ( m_alt )
      return false;

    // react to keyboard cursor keys, page-up, page-down and home
    switch ( code )
    {
      case RIGHT: // right -> arrow key
      case KP_RIGHT:
        checkSelectToFocus();
        if ( m_ctrl )
          m_view.getSelectCell().moveRightEdge();
        else
          m_view.getSelectCell().moveRight();

        if ( m_shift )
          m_view.getFocusCell().moveToVisible();
        else
          m_view.getFocusCell().setPosition( m_view.getSelectCell() );
        return true;

      case LEFT: // left <- arrow key
      case KP_LEFT:
        checkSelectToFocus();
        if ( m_ctrl )
          m_view.getSelectCell().moveLeftEdge();
        else
          m_view.getSelectCell().moveLeft();

        if ( m_shift )
          m_view.getFocusCell().moveToVisible();
        else
          m_view.getFocusCell().setPosition( m_view.getSelectCell() );
        return true;

      case DOWN: // down arrow key
      case KP_DOWN:
        checkSelectToFocus();
        if ( m_ctrl )
          m_view.getSelectCell().moveBottom();
        else
          m_view.getSelectCell().moveDown();

        if ( m_shift )
          m_view.getFocusCell().moveToVisible();
        else
          m_view.getFocusCell().setPosition( m_view.getSelectCell() );
        return true;

      case UP: // up arrow key
      case KP_UP:
        checkSelectToFocus();
        if ( m_ctrl )
          m_view.getSelectCell().moveTop();
        else
          m_view.getSelectCell().moveUp();

        if ( m_shift )
          m_view.getFocusCell().moveToVisible();
        else
          m_view.getFocusCell().setPosition( m_view.getSelectCell() );
        return true;

      case PAGE_DOWN: // page down key
        checkSelectToFocus();
        Utils.trace( "TODO page down", code );
        return true;

      case PAGE_UP: // page up key
        checkSelectToFocus();
        Utils.trace( "TODO page up", code );
        return true;

      case HOME: // home key - navigate to left-most visible column
        checkSelectToFocus();
        Utils.trace( "TODO home", code );
        return true;

      case END: // end key - navigate to right-most visible column
        checkSelectToFocus();
        Utils.trace( "TODO end", code );
        return true;

      default:
        return false;
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

}
