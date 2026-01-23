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

import javafx.scene.Node;
import javafx.scene.Parent;
import rjc.table.signal.ObservableInteger;
import rjc.table.signal.ObservableInteger.ReadOnlyInteger;

/*************************************************************************************************/
/****************************** JavaFX parent node for table views *******************************/
/*************************************************************************************************/

/**
 * A custom JavaFX {@link Parent} node that serves as the container for table view components.
 * <p>
 * This class acts as the root parent node for a table view implementation. It manages the
 * overall size (width and height) of the table area and provides observable properties for 
 * these dimensions. It is designed to fill the available space in its parent layout, allowing
 * scrollbars to appear at the edges of the allocated area when content exceeds the visible region.
 * <p>
 * The node is always resizable and reports extremely large preferred dimensions
 * to encourage its parent container to allocate as much space as possible.
 * <p>
 * This class also provides convenience methods for adding, removing, and checking
 * membership of child nodes that are part of the table's visual representation.
 */
public class TableViewParent extends Parent
{
  private final ObservableInteger m_height = new ObservableInteger(); // current height of the table area in pixels
  private final ObservableInteger m_width  = new ObservableInteger(); // current width of the table area in pixels

  /******************************************* resize ********************************************/
  /**
   * Called by the layout system when this node should be resized to the specified dimensions.
   * <p>
   * Updates the internal observable width and height properties with the new integer values.
   *
   * @param width   the target layout width
   * @param height  the target layout height
   */
  @Override
  public void resize( double width, double height )
  {
    // update observable size properties (cast to int as table operates on pixel integers)
    m_width.set( (int) width );
    m_height.set( (int) height );
  }

  /**************************************** isResizable ******************************************/
  /**
   * Indicates whether this node can be resized by its parent during layout.
   *
   * @return {@code true} — this parent node is always resizable
   */
  @Override
  public boolean isResizable()
  {
    return true;
  }

  /***************************************** minHeight *******************************************/
  /**
   * Returns the minimum height this node is willing to be resized to.
   *
   * @param width the width constraint (ignored in this implementation)
   * @return 0.0 — no minimum height constraint
   */
  @Override
  public double minHeight( double width )
  {
    return 0.0;
  }

  /****************************************** minWidth *******************************************/
  /**
   * Returns the minimum width this node is willing to be resized to.
   *
   * @param height the height constraint (ignored in this implementation)
   * @return 0.0 — no minimum width constraint
   */
  @Override
  public double minWidth( double height )
  {
    return 0.0;
  }

  /***************************************** prefHeight ******************************************/
  /**
   * Returns the preferred height of this node for layout purposes.
   * <p>
   * A very large value is returned so that the parent layout allocates the maximum
   * available vertical space, allowing scrollbars to appear at the edge of the area.
   *
   * @param width the width constraint (ignored in this implementation)
   * @return {@code Integer.MAX_VALUE} to request maximum available height
   */
  @Override
  public double prefHeight( double width )
  {
    return Integer.MAX_VALUE;
  }

  /****************************************** prefWidth ******************************************/
  /**
   * Returns the preferred width of this node for layout purposes.
   * <p>
   * A very large value is returned so that the parent layout allocates the maximum
   * available horizontal space, allowing scrollbars to appear at the edge of the area.
   *
   * @param height the height constraint (ignored in this implementation)
   * @return {@code Integer.MAX_VALUE} to request maximum available width
   */
  @Override
  public double prefWidth( double height )
  {
    return Integer.MAX_VALUE;
  }

  /****************************************** getWidth *******************************************/
  /**
   * Returns the current width of the table area in pixels.
   *
   * @return current integer width
   */
  public int getWidth()
  {
    return m_width.get();
  }

  /****************************************** getHeight ******************************************/
  /**
   * Returns the current height of the table area in pixels.
   *
   * @return current integer height
   */
  public int getHeight()
  {
    return m_height.get();
  }

  /**************************************** widthProperty ****************************************/
  /**
   * Provides read-only access to the width property.
   *
   * @return read-only observable integer property representing the table width
   */
  public ReadOnlyInteger widthProperty()
  {
    return m_width.getReadOnly();
  }

  /**************************************** heightProperty ***************************************/
  /**
   * Provides read-only access to the height property.
   *
   * @return read-only observable integer property representing the table height
   */
  public ReadOnlyInteger heightProperty()
  {
    return m_height.getReadOnly();
  }

  /********************************************* add *********************************************/
  /**
   * Adds a child node to be displayed as part of the table.
   *
   * @param node the node to add to the table's children list
   */
  public void add( Node node )
  {
    getChildren().add( node );
  }

  /******************************************* remove ********************************************/
  /**
   * Removes a child node from the table's displayed content.
   *
   * @param node the node to remove from the table's children list
   */
  public void remove( Node node )
  {
    getChildren().remove( node );
  }

  /****************************************** contains *******************************************/
  /**
   * Checks whether the specified node is currently a child of this table parent.
   *
   * @param node the node to test for membership
   * @return {@code true} if the node is in the children list, otherwise {@code false}
   */
  public boolean contains( Node node )
  {
    return getChildren().contains( node );
  }
}