
"""
Generates ODK survey
Python version: 3.7+
_________________________________________________________________________________________________

Requirements:

1. sty (https://sty.mewo.dev/intro/install.html)
_________________________________________________________________________________________________

Usage:

> python survey_gen.py <Sample color/value>

_________________________________________________________________________________________________

Example commands:

> python survey_gen.py water
> python survey_gen.py soil

_________________________________________________________________________________________________

Notes:

The generated color cards are saved in the CARD_FOLDER folder

_________________________________________________________________________________________________

"""


import sys
import json
import enum
import logging
import xml.etree.ElementTree as ET
from pathlib import Path
from decimal import Context, Decimal
from sty import fg, ef, rs


JSON_FILE_PATH = ''
CARD_FOLDER = 'generated'

class SampleType(enum.Enum):
    Soil = 1
    Water = 2
    Compost = 3

sample_type = SampleType.Soil

error_msg = ''

def print_info(parameter_id, title):
    """Displays color and calibration point info in the console."""

    global error_msg

    print(ef.bold + title + rs.bold_dim + ' : ' + parameter_id)
  

def parse_arguments():
    """Get the user input command line arguments"""

    global card_type
    global sample_type

    sample_type = SampleType.Soil
    if sys.argv[1].casefold().startswith('w'):
        sample_type = SampleType.Water
    elif sys.argv[1].casefold().startswith('c'):
        sample_type = SampleType.Compost


def save_and_export(card, parameter_id):
    """ Save to svg file and also export to other formats if requested"""

    folder = Path(CARD_FOLDER)
    folder.mkdir(exist_ok=True)
    f = open(f'{CARD_FOLDER}/{parameter_id}.csv', 'w')
    f.write(card)
    f.close()


if len(sys.argv) < 1:
    print(fg.red + 'Error: Parameter ID not provided' + fg.rs)
else:

    parse_arguments()

    # Load the appropriate tests json file
    data = json.load(open(JSON_FILE_PATH + 'match-parameters.json'))
    csv = 'type,name,label::English (en),hint,required,appearance,body::intent\n'
    title = ''
    index = 1
    count = 0
    list = ['water_cuvette','water_titration']

    for test_type in list:
        for parameter in data['ffem_match'][test_type]['tests']:
            count += 1
            if count == 1 and len(parameter['results']) == 1:
                csv += f'begin_group,t{index},Tests {index},,0,field-list\n'
                
            parameter_id = parameter['uuid'].casefold().replace('-', '')
            title = parameter['name']

            try:
                parameter_id = parameter['uuid']
                print_info(parameter_id, title)

                intent = ''
                appearance = ''
                if len(parameter['results']) > 1:
                    if count > 1:
                        csv += 'end_group\n'
                    count = 4
                    intent =  f"ffem.match(id='{parameter_id}')"
                    chemical = parameter_id[6:]
                    csv += f"begin_group,{chemical},{title},,0,field-list,{intent}\n"
                    title = parameter['results'][0]['name']
                    csv += f"text,{title.replace(' ', '_')},{title},{parameter['results'][0]['unit']},0,field-list\n"
                    title = parameter['results'][1]['name']
                    csv += f"text,{title.replace(' ', '_')},{title},{parameter['results'][0]['unit']},0,field-list\n"
                    intent = f"ffem.match(id='{parameter_id}')"
                else:
                    appearance = f"ex:ffem.match(id='{parameter_id}')"
                    csv += f"text,{title.replace(' ', '_')},{title},{parameter['results'][0]['unit']},0,{appearance}\n"
            except Exception as e:
                print_info(parameter_id, title)
                logging.error(e, exc_info=True)

            if count == 4:
                csv += 'end_group\n'
                index += 1            
                count = 0
        print('________________________________________________________\n')

    if count < 4:
        csv += 'end_group\n'        
    save_and_export(csv, 'ffem match - Water Tests')

if error_msg != '':
    print('\n' + error_msg  + '\n')
