import pandas as pd
import datetime
import re
import json
from enum import Enum
import sys
from sty import fg, ef, rs

"""
Generates water and soil test reports
Python version: 3.7+
_________________________________________________________________________________________________

Requirements:

1. pandas (https://www.pythoncentral.io/how-to-install-pandas-in-python/)

_________________________________________________________________________________________________

Usage:

Download the survey data csv file from Kobotoolbox and save it as data.csv
Instructions: https://support.kobotoolbox.org/export_download.html

Note: The tag names in the report_template.html has to match the column names in the csv data file

Note: Any manually entered test results for test parameters that is
not in ffem Match json file should be grouped under the group Tests in the survey.
For example column name for a parameter like TDS would appear as Tests/TDS in the csv data file

To generate reports for all rows in the data csv file
> python report_gen.py <sampletype>
Note: defaults to water

example:
> python report_gen.py water
> python report_gen.py soil

To generate a specific report
> python report_gen.py <sampletype> <uuid>
where uuid is the _uuid field in the KoboToolbox csv data file

example: 
> python report_gen.py water e6ce149b-e320-4e4f-8e2e-6cafd6381a40

_________________________________________________________________________________________________

Output:

A HTML file report.html will be generated.
Open the report.html file in a browser and print to a printer or to a pdf file

_________________________________________________________________________________________________

"""

# The name of the csv file downloaded from KoboToolbox
data_file = 'data.csv'

JSON_FILE_PATH = '../ColorCard/'
parameter_json = 'match-parameters.json'

test_group_name = 'Tests'

report_template = 'report_template.html'
row_template = 'row_template.html'
table_template = 'table_template.html'

class SampleType(Enum):
    Soil = 'soil'
    Water = 'water'
    Compost = 'compost'

sample_type = SampleType.Water
if len(sys.argv) > 1:
    input = sys.argv[1]
    try:
        sample_type = SampleType(input.lower())
    except Exception:
        print(fg.red + 'Error: ' + input + ' is not a valid Sample Type' + fg.rs)
        sys.exit()

uuid = ''
if len(sys.argv) > 2:
    uuid = sys.argv[2]

output = '<!DOCTYPE html><html lang="en"><head><title>' + str(sample_type).split('.')[1].capitalize() + ' Test Report</title><link rel="stylesheet" href="style.css"></head><body>'
report_title = str(sample_type).split('.')[1].upper() + ' TEST REPORT'

def formatValue(value):
    formatted = format(float(str(value).replace('>','')), '.2f')
    if str(value).find('>') > -1:
        return '> ' + formatted
    else:
        return formatted

def replace(tag, value, text):
    if pd.isna(value):
        value = ''
    text = text.replace('{{' + tag + '}}', value)
    return text

def find_all_tags (string):
  matches = re.findall ('\\{\\{(.*?)\\}\\}', string)
  return matches

# Read test meta data from tests json file
data = json.load(open(JSON_FILE_PATH + parameter_json))
if sample_type == SampleType.Water:
    parameters = data['ffem_match']['water_card']['tests']
    parameters += data['ffem_match']['water_cuvette']['tests']
    parameters += data['ffem_match']['water_titration']['tests']
else:
    parameters = data['ffem_match']['soil_card']['tests']
    parameters += data['ffem_match']['soil_cuvette']['tests']
    parameters += data['ffem_match']['soil_titration']['tests']    

with open(report_template) as f:
    report_template = f.read()

with open(row_template) as f:
    row_template = f.read()

with open(table_template) as f:
    table_template = f.read()

template_tags = find_all_tags (report_template)

# Read the survey data
try:
    csv_data = pd.read_csv(data_file, sep=",")
except Exception:
    csv_data = pd.read_csv(data_file, sep=";")


# Iterate the rows in the csv data
for index, row in csv_data.iterrows():
    if uuid != '':
        if uuid != row['_uuid']:
            continue

    html = report_template
    
    # Set the title of the report
    html = replace("Title", report_title, html)
    
    table = table_template

    # Create the table of tests and results
    # Iterate the columns in each row of the csv data
    for column in csv_data.columns:
        parameter = ''

        # Get the name of the column and remove group name if it exists
        if column.startswith(test_group_name + '/'):
            groups = column.split('/')
            parameter = groups[len(groups) - 1]
        elif '/' in column:
            groups = column.split('/')
            parameter = groups[len(groups) - 1]
        else:
            parameter = column

        # Ignore any invalid column or a column that is storing only the unit of the parameter
        if parameter != '' and 'Unit' not in parameter and not pd.isna(row[column]):
            parameter_row = row_template
            limit = ''
            unit = ''
        
            # Iterate the tests json file to find supporting info such as limits and unit for the result
            for parameter_data in parameters:
                try:
                    parameter_name = parameter_data['results'][0]['name'].casefold()
                    if parameter_name != parameter.casefold() and len(parameter_data['results']) > 1:
                        parameter_name = parameter_data['results'][1]['name'].casefold()
                except Exception:
                    parameter_name = parameter_data['name'].casefold()
                
                unit = ''
                if parameter_name == parameter.casefold() or column.startswith(test_group_name + '/'):
                    parameter_row = replace("parameter", parameter, parameter_row)
                    parameter_row = replace("result", str(row[column]), parameter_row)

                    if parameter_name == parameter.casefold():
                        try:
                            unit = parameter_data['results'][0]['unit']
                        except Exception:
                            pass
                       
                    parameter_row = replace("limit", limit, parameter_row)
                    parameter_row = replace("unit", unit, parameter_row) 
                    table += parameter_row
                    break

    table += "</table>"
    html = replace("parameters", table, html)

    # Fill in all the other survey data into the report template
    for column in csv_data.columns:
        groups = column.split('/')
        column_name = groups[len(groups) - 1]       
         
        if column_name in template_tags:
            value = row[column]
            try:
                # try formatting to date to check if value represents a date
                if not pd.isna(value):
                    value = datetime.datetime.strptime(value, '%Y-%m-%dT%H:%M:%S').strftime('%d %B %Y')
            except ValueError:
                pass
            html = replace(column_name, value, html)

    # Clean up. Blank out leftover tags in the template
    for field in template_tags:
        html = replace(field, '', html)

    output += html

output += "</body></html>"
file = open("report.html","w")
file.write(output)
file.close()
