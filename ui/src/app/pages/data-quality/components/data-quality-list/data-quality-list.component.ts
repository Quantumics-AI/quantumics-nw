import { Component, OnInit } from '@angular/core';
import { DataQuailtyListResponse } from '../../models/data-quality-list-response';

@Component({
  selector: 'app-data-quality-list',
  templateUrl: './data-quality-list.component.html',
  styleUrls: ['./data-quality-list.component.scss']
})
export class DataQualityListComponent implements OnInit {

  public dataQualityList: Array<DataQuailtyListResponse> = [];

  ngOnInit(): void {

    const dataQuality1 = {
      id: 1,
      ruleName: 'DQ Name1',
      ruleType: 'Data completeness-Row',
      createdBy: 'Naveen',
      createdDate: new Date(),
      modifiedDate: new Date(),
      status: 'Active'

    } as DataQuailtyListResponse;

    this.dataQualityList.push(dataQuality1);
  }
}
