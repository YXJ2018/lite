from Expression import evaluate
from Template import renderList
import StringIO


class Test:
    v=3
    def test(self,x):
        return x*self.v;
    def __getitem3_(self,x):
        return lambda x:x/2;
        
#el = "object.test(123)"
#el = [[VALUE_VAR,"object"],[VALUE_CONSTANTS,"test"],[OP_GET_PROP],[VALUE_NEW_LIST],[VALUE_CONSTANTS,123],[OP_PARAM_JOIN],[OP_INVOKE_METHOD]];
el = [[0, "object"], [-1, "test"], [34], [-3], [-1, 123], [90], [86]];
print(evaluate(el,{"object":Test()}))



def test():
	output = StringIO.StringIO();
	Template(["12345",[0,[1,2,[6]]]]).render({},output)
	print output.getvalue()
