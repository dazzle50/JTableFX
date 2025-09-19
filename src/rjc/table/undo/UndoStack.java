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

package rjc.table.undo;

import java.util.ArrayList;

import rjc.table.Utils;
import rjc.table.signal.ISignal;

/*************************************************************************************************/
/********************************** Stack of UndoCommand objects *********************************/
/*************************************************************************************************/

public class UndoStack implements ISignal
{
  private ArrayList<IUndoCommand> m_stack;      // list of undo commands
  private int                     m_index;      // executed commands index
  private int                     m_cleanIndex; // index when declared clean

  /**************************************** constructor ******************************************/
  public UndoStack()
  {
    // initialise private variables
    m_stack = new ArrayList<>();
    clear();
  }

  /******************************************** clear ********************************************/
  public void clear()
  {
    // clean the stack
    m_stack.clear();
    m_index = 0;
    setClean();
  }

  /****************************************** setClean *******************************************/
  public void setClean()
  {
    // declare current index position as being clean (for example after file saved)
    m_cleanIndex = m_index;
    signal();
  }

  /****************************************** isClean ********************************************/
  public boolean isClean()
  {
    // stack is clean if current index equals clean index
    return m_index == m_cleanIndex;
  }

  /******************************************* getSize *******************************************/
  public int getSize()
  {
    // return stack command list size
    return m_stack.size();
  }

  /******************************************* getIndex ******************************************/
  public int getIndex()
  {
    // return current executed command index
    return m_index;
  }

  /******************************************** push *********************************************/
  public boolean push( IUndoCommand command )
  {
    // if command not valid, then return false and don't push on stack
    if ( !command.isValid() )
      return false;

    // remove any commands from stack that haven't been actioned (i.e. above index)
    if ( m_stack.size() > m_index )
    {
      m_stack.subList( m_index, m_stack.size() ).clear();

      // if stack now shorter than clean-index it will be impossible to return to clean state
      if ( m_stack.size() < m_cleanIndex )
        m_cleanIndex = Integer.MIN_VALUE;
    }

    // add new command to stack, and update stack index
    m_stack.add( command );
    m_index = m_stack.size();
    signal();

    // return true command was successfully added to stack
    return true;
  }

  /******************************************** undo *********************************************/
  public void undo()
  {
    // revert command and decrement index (or no action if nothing to revert)
    if ( m_index > 0 )
    {
      m_index--;
      m_stack.get( m_index ).undo();
      signal();
    }
  }

  /******************************************** redo *********************************************/
  public void redo()
  {
    // action command and increment index (or no action if nothing to redo)
    if ( m_index < m_stack.size() )
    {
      m_stack.get( m_index ).redo();
      m_index++;
      signal();
    }
  }

  /**************************************** getUndoCommand ***************************************/
  public IUndoCommand getUndoCommand()
  {
    // return current undo command (or null if none), useful for when wanting to merging commands
    if ( m_index == 0 )
      return null;
    return m_stack.get( m_index - 1 );
  }

  /***************************************** getUndoText *****************************************/
  public String getUndoText()
  {
    // return text associated with next potential undo (no need to check index as getText does)
    return getText( m_index - 1 );
  }

  /***************************************** getRedoText *****************************************/
  public String getRedoText()
  {
    // return text associated with next potential redo (no need to check index as getText does)
    return getText( m_index );
  }

  /******************************************* getText *******************************************/
  public String getText( int index )
  {
    // return text associated with command at index (or null if outside range)
    if ( index < 0 || index >= m_stack.size() )
      return null;

    // if valid index never return null
    String text = m_stack.get( index ).text();
    return text == null ? "" : text;
  }

  /****************************************** setIndex *******************************************/
  public void setIndex( int index )
  {
    // execute redo's or undo's as necessary to get to target index
    if ( index != m_index )
    {
      while ( index < m_index && m_index > 0 )
      {
        m_index--;
        m_stack.get( m_index ).undo();
      }
      while ( index > m_index && m_index < m_stack.size() )
      {
        m_stack.get( m_index ).redo();
        m_index++;
      }

      // let listeners know that stack have changed
      signal();
    }
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // return as string
    StringBuilder str = new StringBuilder( Utils.name( this ) );
    str.append( "[i=" + m_index );
    str.append( " c=" + m_cleanIndex );
    str.append( " s=" + m_stack.size() );
    for ( IUndoCommand command : m_stack )
      str.append( " \"" + command.text() + "\"" );
    return str.toString() + "]";
  }

}