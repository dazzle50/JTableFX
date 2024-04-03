@echo off
:: /**************************************************************************
::  *  Copyright (C) 2024 by Richard Crook                                   *
::  *  https://github.com/dazzle50/JTableFX                                  *
::  *                                                                        *
::  *  This program is free software: you can redistribute it and/or modify  *
::  *  it under the terms of the GNU General Public License as published by  *
::  *  the Free Software Foundation, either version 3 of the License, or     *
::  *  (at your option) any later version.                                   *
::  *                                                                        *
::  *  This program is distributed in the hope that it will be useful,       *
::  *  but WITHOUT ANY WARRANTY; without even the implied warranty of        *
::  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
::  *  GNU General Public License for more details.                          *
::  *                                                                        *
::  *  You should have received a copy of the GNU General Public License     *
::  *  along with this program.  If not, see http://www.gnu.org/licenses/    *
::  **************************************************************************/

echo ################# JAVA VERISON #################
java -version

if defined PATH_TO_FX (
  echo ################ PATH TO JavaFX ################
  echo %PATH_TO_FX%
) else (
  echo "!!!!!!!!!!!!!!! PATH_TO_FX not defined  !!!!!!!!!!!!!!!"
  pause
  exit 1
)

if exist "%PATH_TO_FX%\javafx.controls.jar" (
  echo JavaFX path is valid
) else (
  echo "!!!!!!!!!!!!!!! PATH_TO_FX not valid  !!!!!!!!!!!!!!!"
  pause
  exit 1
)

echo ################# Starting %* #################
java --module-path "%PATH_TO_FX%" --add-modules=javafx.controls -jar %*

pause