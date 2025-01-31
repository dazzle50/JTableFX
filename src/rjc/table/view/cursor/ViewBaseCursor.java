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
  public static int           x;          // x position on table-view
  public static int           y;          // y position on table-view
  public static TableView     view;       // current table-view
  public static MouseButton   button;     // which if any mouse buttons responsible
  public static ViewPosition  selectCell; // current select cell position
  public static ViewPosition  focusCell;  // current focus cell position
  public static MousePosition mouseCell;  // current mouse cell position
  public static boolean       shift;      // whether or not Shift is pressed
  public static boolean       control;    // whether or not Control is pressed
  public static boolean       alt;        // whether or not Alt is pressed
  public static MouseEvent    event;      // the full mouse-event

  /**************************************** constructor ******************************************/
  public ViewBaseCursor( String imageFile, int xHotspot, int yHotspot )
  {
    // return a cursor based on image file with specified x & y hot spot
    super( new Image( Cursors.class.getResourceAsStream( imageFile ) ), xHotspot, yHotspot );
  }

  /*************************************** extractDetails ****************************************/
  public void extractDetails( MouseEvent mouseEvent )
  {
    // collect data for mouse event handlers
    mouseEvent.consume();
    x = (int) mouseEvent.getX();
    y = (int) mouseEvent.getY();
    view = ( (TableOverlay) mouseEvent.getSource() ).getView();
    button = mouseEvent.getButton();
    selectCell = view.getSelectCell();
    focusCell = view.getFocusCell();
    mouseCell = view.getMouseCell();
    shift = mouseEvent.isShiftDown();
    control = mouseEvent.isControlDown();
    alt = mouseEvent.isAltDown();
    event = mouseEvent;
  }

  /**************************************** handlePressed ****************************************/
  public void handlePressed( MouseEvent event )
  {
    // override as needed
    Utils.trace( "NOT IMPLEMENTED", this );
  }

  /*************************************** handleReleased ****************************************/
  public void handleReleased( MouseEvent event )
  {
    // override as needed
    Utils.trace( "NOT IMPLEMENTED", this );
  }

  /**************************************** handleClicked ****************************************/
  public void handleClicked( MouseEvent event )
  {
    // default do nothing
  }

  /**************************************** handleDragged ****************************************/
  public void handleDragged( MouseEvent event )
  {
    // override as needed
    Utils.trace( "NOT IMPLEMENTED", this );
  }

  /***************************************** isSelecting *****************************************/
  public boolean isSelecting()
  {
    // override as needed
    return false;
  }

  /*************************************** checkScrollingX ***************************************/
  void checkScrollingX()
  {
    // scroll table-view horizontally if cursor beyond sides
    TableScrollBar scrollbar = view.getHorizontalScrollBar();
    int header = view.getHeaderWidth();
    int width = (int) view.getCanvas().getWidth();

    // update or stop view scrolling depending on mouse position
    if ( x >= width && scrollbar.getValue() < scrollbar.getMax() )
      scrollbar.scrollToEnd( x - width );
    else if ( x < header && scrollbar.getValue() > 0.0 )
      scrollbar.scrollToStart( header - x );
    else
      scrollbar.stopAnimationStartEnd();
  }

  /**************************************** checkScrollingY ****************************************/
  void checkScrollingY()
  {
    // determine whether any vertical scrolling needed
    TableScrollBar scrollbar = view.getVerticalScrollBar();
    int header = view.getHeaderHeight();
    int height = (int) view.getCanvas().getHeight();

    // update or stop view scrolling depending on mouse position
    if ( y >= height && scrollbar.getValue() < scrollbar.getMax() )
      scrollbar.scrollToEnd( y - height );
    else if ( y < header && scrollbar.getValue() > 0.0 )
      scrollbar.scrollToStart( header - y );
    else
      scrollbar.stopAnimationStartEnd();
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // return as string
    return getClass().getSimpleName() + "@" + Integer.toHexString( System.identityHashCode( this ) ) + "[" + event
        + "]";
  }

}
