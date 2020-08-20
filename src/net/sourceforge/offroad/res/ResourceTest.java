/** 
   OffRoad
   Copyright (C) 2016 Christian Foltin

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software Foundation,
   Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
*/

package net.sourceforge.offroad.res;

import java.io.File;
import java.io.StringWriter;
import java.util.Locale;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 * Resources generated with 
 * 
 * http://www.freeformatter.com/xsd-generator.html#ad-output
 * 
 * xjc schema.xsd -p net.sourceforge.offroad.res -d /tmp/test
 * 
 * @author foltin
 * @date 16.04.2016
 */
public class ResourceTest {

	/*  schema.xsd

<xs:schema attributeFormDefault="unqualified" elementFormDefault="qualified" xmlns:xs="http://www.w3.org/2001/XMLSchema">
  <xs:element name="resources">
    <xs:complexType>
      <xs:sequence>
        <xs:element name="string" maxOccurs="unbounded" minOccurs="0">
          <xs:complexType>
            <xs:simpleContent>
              <xs:extension base="xs:string">
                <xs:attribute type="xs:string" name="name" use="optional"/>
              </xs:extension>
            </xs:simpleContent>
          </xs:complexType>
        </xs:element>
      </xs:sequence>
    </xs:complexType>
  </xs:element>
</xs:schema>

	 */
	public static void main(String[] args) throws JAXBException {
		Resources test = new Resources();
		net.sourceforge.offroad.res.Resources.String res = new Resources.String();
		res.setName("bla");
		res.setValue("inhaltäöO");
		test.getString().add(res);
		JAXBContext jc = JAXBContext.newInstance(Resources.class);
		Unmarshaller u = jc.createUnmarshaller();
		Resources element = (Resources) u.unmarshal(new File("/home/foltin/programming/java/osmand/Osmand/OsmAnd/res/values-es/strings.xml"));
		Marshaller m = jc.createMarshaller();
		StringWriter writer = new StringWriter();
		m.marshal(element, writer);
		System.out.println(writer.toString().substring(0, 2000));
		System.out.println(		Locale.getDefault().getCountry());
	}

}
