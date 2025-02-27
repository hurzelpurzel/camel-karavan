/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import React, {useState} from 'react';
import '../designer/karavan.css';
import {Flex, FlexItem, PageSection, Switch, Tab, Tabs, Text, TextContent, TextInput, Toolbar, ToolbarContent, ToolbarItem} from "@patternfly/react-core";
import {MainToolbar} from "../designer/MainToolbar";
import {KameletsTab} from "./kamelets/KameletsTab";
import {EipTab} from "./eip/EipTab";
import {ComponentsTab} from "./components/ComponentsTab";

interface Props {
    dark: boolean,
}

export const KnowledgebasePage = (props: Props) => {

    const [tab, setTab] = useState<string | number>("kamelets");
    const [filter, setFilter] = useState<string>("");
    const [customOnly, setCustomOnly] = useState<boolean>(false);

    function title() {
        return <TextContent>
            <Text component="h2">Knowledgebase</Text>
        </TextContent>
    }

    function getTools() {
        return <Toolbar id="toolbar-group-types">
            <ToolbarContent>
                {tab === 'kamelets' && <ToolbarItem>
                    <Switch
                        label="Custom only"
                        isChecked={customOnly}
                        onChange={checked => setCustomOnly(checked)}
                    />
                </ToolbarItem>}
                <ToolbarItem>
                    <TextInput className="text-field" type="search" id="search" name="search"
                               value={filter}
                               onChange={value => setFilter(value)}
                               autoComplete="off"
                               placeholder="Search by name"/>
                </ToolbarItem>
            </ToolbarContent>
        </Toolbar>
    }

    return (
        <PageSection className="kamelet-section" padding={{default: 'noPadding'}}>
            <PageSection className="tools-section" padding={{default: 'noPadding'}}>
                <MainToolbar title={title()} tools={getTools()}/>
            </PageSection>
            <PageSection className="tools-section" padding={{default: 'noPadding'}}>
                <Flex direction={{default: "column"}} spaceItems={{default: "spaceItemsNone"}}>
                    <FlexItem className="knowledge-tabs">
                        <Tabs activeKey={tab} onSelect={(event, tabIndex) => setTab(tabIndex)}>
                            <Tab eventKey="kamelets" title="Kamelets"/>
                            <Tab eventKey="eip" title="Enterprise Integration Patterns"/>
                            <Tab eventKey="components" title="Components"/>
                        </Tabs>
                    </FlexItem>
                </Flex>
            </PageSection>
            <>
                {tab === 'kamelets' && <KameletsTab dark={props.dark} filter={filter} customOnly={customOnly}/>}
                {tab === 'eip' && <EipTab dark={props.dark} filter={filter}/>}
                {tab === 'components' && <ComponentsTab dark={props.dark} filter={filter}/>}
            </>
        </PageSection>
    )

}