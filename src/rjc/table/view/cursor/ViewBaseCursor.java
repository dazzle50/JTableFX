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

import javafx.scene.ImageCursor;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import rjc.table.Utils;
import rjc.table.view.TableOverlay;
import rjc.table.view.TableScrollBar;
import rjc.table.view.TableView;
import rjc.table.view.cell.MousePosition;
import rjc.table.view.cell.ViewPosition;

/*************************************************************************************************/
/********************** Base class for specialised table-view mouse cursors **********************/
/*************************************************************************************************/

public class ViewBaseCursor extends ImageCursor
{
  static int           m_x;          // x position on table-view
  static int           m_y;          // y position on table-view
  static TableView     m_view;       // current table-view
  static MouseButton   m_button;     // which if any mouse buttons responsible
  static ViewPosition  m_selectCell; // current select cell position
  static ViewPosition  m_focusCell;  // current focus cell position
  static MousePosition m_mouseCell;  // current mouse cell position
  static boolean       m_shift;      // whether or not Shift is pressed
  static boolean       m_control;    // whether or not Control is pressed
  static boolean       m_alt;        // whether or not Alt is pressed
  static MouseEvent    m_event;      // the full mouse-event

  /**************************************** constructor ******************************************/
  public ViewBaseCursor( String imageFile, int xHotspot, int yHotspot )
  {
    // return a cursor based on image file with specified x & y hot spot
    super( new Image( Cursors.class.getResourceAsStream( imageFile ) ), xHotspot, yHotspot );
  }

  /**************************************** constructor ******************************************/
  public ViewBaseCursor()
  {
    // return a cursor which looks like Cursor.DEFAULT
  }

  /*************************************** extractDetails ****************************************/
  void extractDetails( MouseEvent mouseEvent )
  {
    // collect data for mouse event handlers
    mouseEvent.consume();
    m_x = (int) mouseEvent.getX();
    m_y = (int) mouseEvent.getY();
    m_view = ( (TableOverlay) mouseEvent.getSource() ).getView();
    m_button = mouseEvent.getButton();
    m_selectCell = m_view.getSelectCell();
    m_focusCell = m_view.getFocusCell();
    m_mouseCell = m_view.getMouseCell();
    m_shift = mouseEvent.isShiftDown();
    m_control = mouseEvent.isControlDown();
    m_alt = mouseEvent.isAltDown();
    m_event = mouseEvent;
  }

  /**************************************** handlePressed ****************************************/
  public void handlePressed( MouseEvent event )
  {
    // default do nothing
  }

  /*************************************** handleReleased ****************************************/
  public void handleReleased( MouseEvent event )
  {
    // default do nothing
  }

  /**************************************** handleClicked ****************************************/
  public void handleClicked( MouseEvent event )
  {
    // default do nothing
  }

  /**************************************** handleDragged ****************************************/
  public void handleDragged( MouseEvent event )
  {
    // default do nothing
  }

  /**************************************** tableScrolled ****************************************/
  public void tableScrolled()
  {
    // update select cell position only if cursor is selecting
    if ( isSelecting() )
      m_view.checkSelectPosition();
  }

  /***************************************** isSelecting *****************************************/
  public boolean isSelecting()
  {
    // default cursor is NOT selecting
    return false;
  }

  /*************************************** checkScrollingX ***************************************/
  void checkScrollingX()
  {
    // scroll table-view horizontally if cursor beyond sides
    TableScrollBar scrollbar = m_view.getHorizontalScrollBar();
    int header = m_view.getHeaderWidth();
    int width = (int) m_view.getCanvas().getWidth();

    // update or stop view scrolling depending on mouse position
    if ( m_x >= width && scrollbar.getValue() < scrollbar.getMax() )
      scrollbar.scrollToEnd( m_x - width );
    else if ( m_x < header && scrollbar.getValue() > 0.0 )
      scrollbar.scrollToStart( header - m_x );
    else
      scrollbar.stopAnimationStartEnd();
  }

  /**************************************** checkScrollingY ****************************************/
  void checkScrollingY()
  {
    // determine whether any vertical scrolling needed
    TableScrollBar scrollbar = m_view.getVerticalScrollBar();
    int header = m_view.getHeaderHeight();
    int height = (int) m_view.getCanvas().getHeight();

    // update or stop view scrolling depending on mouse position
    if ( m_y >= height && scrollbar.getValue() < scrollbar.getMax() )
      scrollbar.scrollToEnd( m_y - height );
    else if ( m_y < header && scrollbar.getValue() > 0.0 )
      scrollbar.scrollToStart( header - m_y );
    else
      scrollbar.stopAnimationStartEnd();
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // return as string
    return Utils.name( this ) + "[" + m_event + "]";
  }

}
