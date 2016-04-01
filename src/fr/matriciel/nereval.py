'''
Created on 18 mars 2016

@author: Capeyron, Domingues & Brando

@summary: Evaluation of the task for named-entity detection (classification is not considered) according to the State of art (Nouvel et al. 2015)
The input texts should fulfill the following requirements:
- parenthesis are only for indicating the presence of geographic coordinates which are actually not mandatory.
- line feeds may be present but should be removed, the script does it

'''

import sys
import re

'''Function definitions and invocation'''

def extractENphrases(removeLineFeed, removeGeocord, fileN):
    with open(fileN, 'r') as content_file:
        content = content_file.read()    
        
        # remove line feed, typically for gold file
        if removeLineFeed:
            content = content.replace('\n', '').rstrip()            
        
        # match geo-coordinates and remove them
        if removeGeocord:
            patGeocoord = re.compile(r"\([^\)]+\)")        
            content = patGeocoord.sub("", content)
            text_file = open("nolines.txt", "w")
            text_file.write(content)
            text_file.close()
            
        content = open("nolines.txt", 'r').read()
        
        # match ENS related text segment and capture groups
        pat = re.compile("\{([^}]+)\}")
        maintup = ();
        countC = 0
        for m in pat.finditer(content):
            valENS = m.group(0)
            print valENS
            print "START: "+str(m.span())
            
            ini = m.start() - countC
            print 
            #print "countAVANT: "+str(countC)
            #print "iniF: "+str(ini)                    
            countC = countC + valENS.count('}')+valENS.count('{')+valENS.count('(')+valENS.count(')')+valENS.count(']')+valENS.count('[')
            fin = m.end() - countC
            singletup = (ini,valENS,fin)
            #print "countAPRES: "+str(countC)
            #print "finF: "+str(fin)
            #print singletup
            maintup = (singletup,)
    return maintup

def rappel():    
    return 0

def precision():
    return 0

def fscore():
    return 0

def err():
    return 0

'''Main program'''

if len(sys.argv) > 1:
    # Usage: python main.py goldFile annotFile
    goldFile = sys.argv[1]
    annotFile = sys.argv[2]
    
else:
    print "Usage: python main.py goldFile annotFile"

print "BEGIN"

# C : total number of annotated objects in the manually annotated file that are correct
c = 0
# I : total number of adds performed by the system, in other words, objects that are not named-entities but the system considered as ones
i = 0
# D: total number of omissions (deletions) performed by the system, in other words, non-detected entities
d = 0

goldPhases = extractENphrases(True, True, goldFile)
#annotPhrases = extractENphrases(False, False, annotFile)
overlappingDegree = 5

#for phrase in goldPhases:
    

print "END"