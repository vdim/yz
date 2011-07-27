DOC	= document
TEXSRC	= *.tex
IMGSRC	= 
COMPILE_SCRIPT = compile_doc.sh

all: $(DOC).pdf

ps: $(DOC).ps

$(DOC).pdf: $(DOC).tex $(TEXSRC) $(IMGSRC) 
	$(COMPILE_SCRIPT) $(DOC)

$(DOC).ps: $(DOC).pdf
	pdftops $(DOC).pdf $(DOC).ps


clean:
	@rm -v -rf $(DOC).log $(DOC).toc $(DOC).aux $(DOC).out $(DOC).pdf $(DOC).bbl $(DOC).blg

ispell:
	ispell -d russian *tex

aspell:
	bash -c 'for i in *tex; do aspell -d ru -t -c $$i; done'
